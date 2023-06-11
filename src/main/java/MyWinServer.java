import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;
public class MyWinServer extends JFrame {

	private static final long serialVersionUID = 1L;
	
	//private JTextArea m_outTextArea;
	private JTextPane m_outTextPane;
	private JTextField m_inTextField;
	private JButton m_startStopButton;
	private CMServerStub m_serverStub;
	private MyWinServerEventHandler m_eventHandler;
	private JList<String> m_fileList;  // 추가된 부분: 파일 목록을 표시할 JList

	private DefaultListModel<String> m_fileListModel;
	private String userName;

	MyWinServer()
	{
		userName=null;
		MyKeyListener cmKeyListener = new MyKeyListener();
		MyActionListener cmActionListener = new MyActionListener();
		setTitle("My Server");
		setSize(500, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setMenus();
		setLayout(new BorderLayout());
		
		m_outTextPane = new JTextPane();
		m_outTextPane.setEditable(false);

		StyledDocument doc = m_outTextPane.getStyledDocument();
		addStylesToDocument(doc);

		add(m_outTextPane, BorderLayout.CENTER);
		JScrollPane scroll = new JScrollPane (m_outTextPane, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		add(scroll);
		
		m_inTextField = new JTextField();
		m_inTextField.addKeyListener(cmKeyListener);
		add(m_inTextField, BorderLayout.SOUTH);
		
		JPanel topButtonPanel = new JPanel();
		topButtonPanel.setLayout(new FlowLayout());
		add(topButtonPanel, BorderLayout.NORTH);
		
		m_startStopButton = new JButton("Start Server CM");
		m_startStopButton.addActionListener(cmActionListener);
		m_startStopButton.setEnabled(false);
		//add(startStopButton, BorderLayout.NORTH);
		topButtonPanel.add(m_startStopButton);
		
		setVisible(true);

		// create CM stub object and set the event handler
		m_serverStub = new CMServerStub();
		m_eventHandler = new MyWinServerEventHandler(m_serverStub, this);

		// 추가된 부분: 파일 목록을 저장할 DefaultListModel 초기화
		m_fileListModel = new DefaultListModel<>();
		m_fileList = new JList<>(m_fileListModel);
		JScrollPane fileListScrollPane = new JScrollPane(m_fileList);
		add(fileListScrollPane, BorderLayout.EAST);

		// 추가된 부분: 파일 목록 갱신을 위한 타이머 설정

			Timer timer = new Timer(1000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateFileList(userName);
					// 파일 및 디렉토리 업데이트 로직 작성
				}
			});
			timer.start();
		setVisible(true);
		// start cm
		startCM();		
	}


	private void addStylesToDocument(StyledDocument doc)
	{
		Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style regularStyle = doc.addStyle("regular", defStyle);
		StyleConstants.setFontFamily(regularStyle, "SansSerif");
		
		Style boldStyle = doc.addStyle("bold", defStyle);
		StyleConstants.setBold(boldStyle, true);
	}
	
	public CMServerStub getServerStub()
	{
		return m_serverStub;
	}
	
	public MyWinServerEventHandler getServerEventHandler()
	{
		return m_eventHandler;
	}
	
	public void setMenus()
	{
		MyMenuListener menuListener = new MyMenuListener();
		JMenuBar menuBar = new JMenuBar();

		JMenu helpMenu = new JMenu("server file");
		//helpMenu.setMnemonic(KeyEvent.VK_H);
		JMenuItem checkfileItem = new JMenuItem("check file-path");
		checkfileItem.addActionListener(menuListener);
		checkfileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));

		helpMenu.add(checkfileItem);
		menuBar.add(helpMenu);
		
		setJMenuBar(menuBar);
	}
	private void updateFileList(String username) {
		Path serverFilePath = Path.of(m_serverStub.getTransferedFileHome() + "/" + username);
		if (serverFilePath != null) {
			File[] files = serverFilePath.toFile().listFiles();
			m_fileListModel.clear();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						m_fileListModel.addElement(file.getName());
					}
				}
			}
		}
	}
	
	public void processInput(String strInput)
	{

		
		switch(strInput)
		{
		case "check":
			check_file_path();
			break;
//		case 70:	// open file-sync folder
//			openFileSyncFolder();
//			break;

		default:
			printStyledMessage("알 수 없는 명령어입니다.\n", "bold");
			break;
		}
	}
	
	private void check_file_path()
	{
		printMessage("===========================\n");
		printMessage("open server file path folder\n");
		// ask client name
		userName = JOptionPane.showInputDialog("User Name:");
		if(userName != null) {
			// get the file-sync home of "userName"
			Path serverfile = Path.of(m_serverStub.getTransferedFileHome() + "/" + userName);
			if(serverfile == null) {
				printStyledMessage("File sync home is null!\n", "bold");
				printStyledMessage("Please see error message on console for more information.\n",
						"bold");
				return;
			}
			// open syncHome folder
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.open(serverfile.toFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
			updateFileList(userName);
		}
	}

	public void startCM()
	{
		boolean bRet = false;
		
		// get current server info from the server configuration file
		String strSavedServerAddress = null;
		List<String> localAddressList = null;
		int nSavedServerPort = -1;
		
		// set config home
		m_serverStub.setConfigurationHome(Paths.get("."));
		// set file-path home
		m_serverStub.setTransferedFileHome(m_serverStub.getConfigurationHome().resolve("server-file-path"));

		localAddressList = CMCommManager.getLocalIPList();
		if(localAddressList == null) {
			System.err.println("Local address not found!");
			return;
		}
		strSavedServerAddress = m_serverStub.getServerAddress();
		nSavedServerPort = m_serverStub.getServerPort();
		
		// ask the user if he/she would like to change the server info
		JTextField myCurrentAddressTextField = new JTextField(localAddressList.get(0).toString());
		myCurrentAddressTextField.setEnabled(false);
		JTextField serverAddressTextField = new JTextField(strSavedServerAddress);		
		JTextField serverPortTextField = new JTextField(String.valueOf(nSavedServerPort));
		Object msg[] = {
				"My Current Address:", myCurrentAddressTextField,
				"Server Address:", serverAddressTextField, 
				"Server Port:", serverPortTextField

		};
		int option = JOptionPane.showConfirmDialog(null, msg, "Server Information", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);

		// update the server info if the user would like to do
		if (option == JOptionPane.OK_OPTION)
		{
			String strNewServerAddress = serverAddressTextField.getText().toString();
			int nNewServerPort = Integer.parseInt(serverPortTextField.getText());
			if(!strNewServerAddress.equals(strSavedServerAddress) || nNewServerPort != nSavedServerPort)
				m_serverStub.setServerInfo(strNewServerAddress, nNewServerPort);
		}
		
		// start cm
		bRet = m_serverStub.startCM();
		if(!bRet)
		{
			printStyledMessage("CM initialization error!\n", "bold");
		}
		else
		{
			printStyledMessage("Server CM starts.\n", "bold");
			printMessage("Type \"0\" for menu.\n");					
			// change button to "stop CM"
			m_startStopButton.setEnabled(true);
			m_startStopButton.setText("Stop Server CM");
		}

		m_inTextField.requestFocus();

	}
	
	public void terminateCM()
	{
		m_serverStub.terminateCM();
		printMessage("Server CM terminates.\n");
		m_startStopButton.setText("Start Server CM");
	}

	public void printMessage(String strText)
	{
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, null);
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public void printStyledMessage(String strText, String strStyleName)
	{
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, doc.getStyle(strStyleName));
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return;
	}

	public class MyKeyListener implements KeyListener {
		public void keyPressed(KeyEvent e)
		{
			int key = e.getKeyCode();
			if(key == KeyEvent.VK_ENTER)
			{
				JTextField input = (JTextField)e.getSource();
				String strText = input.getText();
				printMessage(strText+"\n");
				// parse and call CM API
				processInput(strText);
				input.setText("");
				input.requestFocus();
			}
		}
		
		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
	}
	
	public class MyActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			JButton button = (JButton) e.getSource();
			if(button.getText().equals("Start Server CM"))
			{
				// start cm
				boolean bRet = m_serverStub.startCM();
				if(!bRet)
				{
					printStyledMessage("CM initialization error!\n", "bold");
				}
				else
				{
					printStyledMessage("Server CM starts.\n", "bold");
					printMessage("Type \"0\" for menu.\n");					
					// change button to "stop CM"
					button.setText("Stop Server CM");
				}
				// check if default server or not
				if(CMConfigurator.isDServer(m_serverStub.getCMInfo()))
				{
					setTitle("CM Default Server (\"SERVER\")");
				}
				else
				{
					setTitle("CM Additional Server (\"?\")");
				}					
				m_inTextField.requestFocus();
			}
			else if(button.getText().equals("Stop Server CM"))
			{
				// stop cm
				m_serverStub.terminateCM();
				printMessage("Server CM terminates.\n");
				// change button to "start CM"
				button.setText("Start Server CM");
			}
		}
	}
	
	public class MyMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String strMenu = e.getActionCommand();
			switch(strMenu)
			{
			case "check file-path":
				check_file_path();
				break;
			case "start CM":
				startCM();
				break;
			case "terminate CM":
				terminateCM();
				break;
			}
		}
	}

	public static void main(String[] args)
	{
		MyWinServer server = new MyWinServer();
		CMServerStub cmStub = server.getServerStub();
		cmStub.setAppEventHandler(server.getServerEventHandler());
	}
}
