
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.manager.*;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.Timer;

public class MyWinClient extends JFrame {

	private static final long serialVersionUID = 1L;
	//private JTextArea m_outTextArea;
	private JTextPane m_outTextPane;
	private JTextField m_inTextField;
	private JButton m_startStopButton;
	private JButton m_loginLogoutButton;
	private MyMouseListener cmMouseListener;
	private CMClientStub m_clientStub;
	private static MyWinClientEventHandler m_eventHandler;
	private static String strUserName;
	private static Timer timer;
	private JList<String> m_fileList;  // 추가된 부분: 파일 목록을 표시할 JList
	private DefaultListModel<String> m_fileListModel;

	MyWinClient()
	{
		strUserName=null;
		MyKeyListener cmKeyListener = new MyKeyListener();
		MyActionListener cmActionListener = new MyActionListener();
		cmMouseListener = new MyMouseListener();
		setTitle("CM Client");
		setSize(600, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setMenus();
		setLayout(new BorderLayout());

		m_outTextPane = new JTextPane();
		m_outTextPane.setBackground(new Color(245,245,245));
		//m_outTextPane.setForeground(Color.WHITE);
		m_outTextPane.setEditable(false);

		StyledDocument doc = m_outTextPane.getStyledDocument();
		addStylesToDocument(doc);
		add(m_outTextPane, BorderLayout.CENTER);
		JScrollPane centerScroll = new JScrollPane (m_outTextPane,
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//add(centerScroll);
		getContentPane().add(centerScroll, BorderLayout.CENTER);

		m_inTextField = new JTextField();
		m_inTextField.addKeyListener(cmKeyListener);
		add(m_inTextField, BorderLayout.SOUTH);

		JPanel topButtonPanel = new JPanel();
		topButtonPanel.setBackground(new Color(220,220,220));
		topButtonPanel.setLayout(new FlowLayout());
		add(topButtonPanel, BorderLayout.NORTH);

		m_startStopButton = new JButton("Start Client CM");
		//m_startStopButton.setBackground(Color.LIGHT_GRAY);	// not work on Mac
		m_startStopButton.addActionListener(cmActionListener);
		m_startStopButton.setEnabled(false);
		//add(startStopButton, BorderLayout.NORTH);
		topButtonPanel.add(m_startStopButton);

		m_loginLogoutButton = new JButton("Login");
		m_loginLogoutButton.addActionListener(cmActionListener);
		m_loginLogoutButton.setEnabled(false);
		topButtonPanel.add(m_loginLogoutButton);

		setVisible(true);

		// create a CM object and set the event handler
		m_clientStub = new CMClientStub();
		m_eventHandler = new MyWinClientEventHandler(m_clientStub, this);

		// 추가된 부분: 파일 목록을 저장할 DefaultListModel 초기화
		m_fileListModel = new DefaultListModel<>();
		m_fileList = new JList<>(m_fileListModel);
		JScrollPane fileListScrollPane = new JScrollPane(m_fileList);
		m_fileList.addListSelectionListener(new FileListSelectionListener());
		add(fileListScrollPane, BorderLayout.EAST);

		// 추가된 부분: 파일 목록 갱신을 위한 타이머 설정

		javax.swing.Timer timer = new javax.swing.Timer(3000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// 파일 및 디렉토리 업데이트 로직 작성
				updateFileList();
			}
		});
		timer.start();

		// start CM
		testStartCM();
		m_inTextField.requestFocus();
	}

	private class FileListSelectionListener implements ListSelectionListener {  //파일 리스트를 클릭 했을 때 이벤트 발생
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				String selectedFile = m_fileList.getSelectedValue();
				if (selectedFile != null) {
					showFileContents(selectedFile);
				}
			}
		}
	}
	private void showFileContents(String fileName) {
		// 파일 내용을 가져와서 표시하는 로직
		try {
			String filePath = m_clientStub.getTransferedFileHome() + "/"+strUserName+"/" + fileName;
			FileReader fileReader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			StringBuilder contents = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				contents.append(line).append("\n");
			}
			bufferedReader.close();

			// 파일 내용을 JTextPane에 표시
			printMessage("====== file content\n");
			printMessage(contents.toString()+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	private void updateFileList() {
		Path serverFilePath = Path.of(m_clientStub.getTransferedFileHome() + "/" + strUserName);
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

	private void addStylesToDocument(StyledDocument doc)
	{
		Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style regularStyle = doc.addStyle("regular", defStyle);
		StyleConstants.setFontFamily(regularStyle, "SansSerif");

		Style boldStyle = doc.addStyle("bold", defStyle);
		StyleConstants.setBold(boldStyle, true);

		Style linkStyle = doc.addStyle("link", defStyle);
		StyleConstants.setForeground(linkStyle, Color.BLUE);
		StyleConstants.setUnderline(linkStyle, true);
	}

	private CMClientStub getClientStub()
	{
		return m_clientStub;
	}

	private MyWinClientEventHandler getClientEventHandler()
	{
		return m_eventHandler;
	}

	// set menus
	private void setMenus()
	{
		MyMenuListener menuListener = new MyMenuListener();
		JMenuBar menuBar = new JMenuBar();

		JMenu helpMenu = new JMenu("Help");
		//helpMenu.setMnemonic(KeyEvent.VK_H);
		JMenuItem showAllMenuItem = new JMenuItem("show all menus");
		showAllMenuItem.addActionListener(menuListener);
		showAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));

		helpMenu.add(showAllMenuItem);
		menuBar.add(helpMenu);

		JMenu cmServiceMenu = new JMenu("Services");

		JMenu fileTransferSubMenu = new JMenu("File Transfer");

		JMenuItem currentdirecttorycheckItem = new JMenuItem("current directory check");
		currentdirecttorycheckItem.addActionListener(menuListener);
		fileTransferSubMenu.add(currentdirecttorycheckItem);

		JMenuItem createFileItem = new JMenuItem("create file");
		createFileItem.addActionListener(menuListener);
		createFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		fileTransferSubMenu.add(createFileItem);

		JMenuItem deleteFileMenuItem = new JMenuItem("delete file");
		deleteFileMenuItem.addActionListener(menuListener);
		deleteFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
		fileTransferSubMenu.add(deleteFileMenuItem);

		JMenuItem modifyFileMenuItem = new JMenuItem("modify file");
		modifyFileMenuItem.addActionListener(menuListener);
		fileTransferSubMenu.add(modifyFileMenuItem);

		JMenuItem shareMenuItem = new JMenuItem("share");
		shareMenuItem.addActionListener(menuListener);
		fileTransferSubMenu.add(shareMenuItem);

		cmServiceMenu.add(fileTransferSubMenu);
		;
		menuBar.add(cmServiceMenu);

		setJMenuBar(menuBar);

	}

	// initialize button titles
	private void initializeButtons()
	{
		m_startStopButton.setText("Start Client CM");
		m_loginLogoutButton.setText("Login");
		revalidate();
		repaint();
	}
	// set button titles
	public void setButtonsAccordingToClientState()
	{
		int nClientState;
		nClientState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();

		// nclientState: CMInfo.CM_INIT, CMInfo.CM_CONNECT, CMInfo.CM_LOGIN, CMInfo.CM_SESSION_JOIN
		switch(nClientState)
		{
			case CMInfo.CM_INIT:
				m_startStopButton.setText("Stop Client CM");
				m_loginLogoutButton.setText("Login");
				//m_leftButtonPanel.setVisible(false);
				//m_westScroll.setVisible(false);
				break;
			case CMInfo.CM_CONNECT:
				m_startStopButton.setText("Stop Client CM");
				m_loginLogoutButton.setText("Login");
				//m_leftButtonPanel.setVisible(false);
				//m_westScroll.setVisible(false);
				break;
			case CMInfo.CM_LOGIN:
				m_startStopButton.setText("Stop Client CM");
				m_loginLogoutButton.setText("Logout");
				//m_leftButtonPanel.setVisible(false);
				//m_westScroll.setVisible(false);
				break;
			case CMInfo.CM_SESSION_JOIN:
				m_startStopButton.setText("Stop Client CM");
				m_loginLogoutButton.setText("Logout");
				//m_leftButtonPanel.setVisible(true);
				//m_westScroll.setVisible(true);
				break;
			default:
				m_startStopButton.setText("Start Client CM");
				m_loginLogoutButton.setText("Login");
				//m_leftButtonPanel.setVisible(false);
				//m_westScroll.setVisible(false);
				break;
		}
		revalidate();
		repaint();
	}

	public void printMessage(String strText)
	{
		/*
		m_outTextArea.append(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
		*/
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

	public void printImage(String strPath)
	{
		int nTextPaneWidth = m_outTextPane.getWidth();
		int nImageWidth;
		int nImageHeight;
		int nNewWidth;
		int nNewHeight;

		File f = new File(strPath);
		if(!f.exists())
		{
			printMessage(strPath+"\n");
			return;
		}

		ImageIcon icon = new ImageIcon(strPath);
		Image image = icon.getImage();
		nImageWidth = image.getWidth(m_outTextPane);
		nImageHeight = image.getHeight(m_outTextPane);

		if(nImageWidth > nTextPaneWidth/2)
		{
			nNewWidth = nTextPaneWidth / 2;
			float fRate = (float)nNewWidth/(float)nImageWidth;
			nNewHeight = (int)(nImageHeight * fRate);
			Image newImage = image.getScaledInstance(nNewWidth, nNewHeight, Image.SCALE_SMOOTH);
			icon = new ImageIcon(newImage);
		}

		m_outTextPane.insertIcon ( icon );
		printMessage("\n");
	}

	public void printFilePath(String strPath)
	{
		JLabel pathLabel = new JLabel(strPath);
		pathLabel.addMouseListener(cmMouseListener);
		m_outTextPane.insertComponent(pathLabel);
		printMessage("\n");
	}

	private void processInput(String strInput) throws IOException {
		switch (strInput) {
			case "print all menus":
				printAllMenus();
				break;
			case "start cm":
				testStartCM();
				break;
			case "terminate cm":
				testTerminateCM();
				break;
			case "connect to ds": // connect to default server
				testConnectionDS();
				break;
			case "disconnect": // disconnect from default server
				testDisconnectionDS();
				break;
			case "login": // asynchronous login to default server
				testLoginDS();
				break;
			case "logout": // logout from default server
				testLogoutDS();
				break;
			case "chat": // chat
				testChat();
				break;
			case "upload": // push a file
				uploadMultipleFiles(m_clientStub);
				break;
			case "get list": // 디렉토리 내 파일 접근
				printMessage(getFilesInClientDirectory(m_clientStub).toString());
				break;
			case "request":
				request(m_clientStub);
				break;
			case "modify file":
				ModifyFile(m_clientStub);
				break;
			case "create file":
				Createfile(m_clientStub);
				break;
			case "delete file":
				DeleteFile(m_clientStub);
				break;
			case "share":
				ShareFile(m_clientStub);
				break;
			case "get content":
				get_file_content(m_clientStub);
			default:
				printMessage("다시 입력해주세요");
				break;
		}
	}

	private void printAllMenus()
	{
		printMessage("---------------------------------- Help\n");
		printMessage("show all menus\n");
		printMessage("---------------------------------- Start/Stop\n");
		printMessage("request\n");
		printMessage("create file\n");
		printMessage("get list\n");
		printMessage("modify file\n");
		printMessage("delete file\n");
		printMessage("share\n");
		printMessage("upload\n");
		printMessage("chat\n");
		printMessage("start cm\n");
		printMessage("terminate cm\n");
		printMessage("connect to ds\n");
		printMessage("disconnect\n");
		printMessage("login\n");
		printMessage("logout\n");

	}
	private void testTerminateCM()
	{
		//m_clientStub.disconnectFromServer();
		m_clientStub.terminateCM();
		printMessage("Client CM terminates.\n");
		// change the appearance of buttons in the client window frame
		initializeButtons();
		setTitle("CM Client");
	}
	private void testConnectionDS()
	{
		printMessage("====== connect to default server\n");
		boolean ret = m_clientStub.connectToServer();
		if(ret)
		{
			printMessage("Successfully connected to the default server.\n");
		}
		else
		{
			printMessage("Cannot connect to the default server!\n");
		}
		printMessage("======\n");
	}

	private void testDisconnectionDS()
	{
		printMessage("====== disconnect from default server\n");
		boolean ret = m_clientStub.disconnectFromServer();
		if(ret)
		{
			printMessage("Successfully disconnected from the default server.\n");
		}
		else
		{
			printMessage("Error while disconnecting from the default server!");
		}
		printMessage("======\n");

		setButtonsAccordingToClientState();
		setTitle("CM Client");
	}

	private void testLoginDS()
	{
		String strPassword = null;
		boolean bRequestResult = false;

		printMessage("====== login to default server\n");
		JTextField userNameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();
		Object[] message = {
				"User Name:", userNameField,
				"Password:", passwordField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Login Input", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			strUserName = userNameField.getText();
			strPassword = new String(passwordField.getPassword()); // security problem?
			;
			bRequestResult = m_clientStub.loginCM(strUserName, strPassword);
			if(bRequestResult)
			{
				printMessage("successfully sent the login request.\n");
				setButtonsAccordingToClientState();
			}
			else
			{
				printStyledMessage("failed the login request!\n", "bold");
			}

		}
		// Change the title of the login button
		setButtonsAccordingToClientState();
//		printMessage("======\n");
	}

	private void testLogoutDS()
	{
		boolean bRequestResult = false;
		printMessage("====== logout from default server\n");
		bRequestResult = m_clientStub.logoutCM();
		if(bRequestResult)
			printMessage("successfully sent the logout request.\n");
		else
			printStyledMessage("failed the logout request!\n", "bold");
		printMessage("======\n");

		// Change the title of the login button
		setButtonsAccordingToClientState();
		setTitle("CM Client");
	}

	private void testStartCM()
	{
		boolean bRet = false;

		// get local address
		List<String> localAddressList = CMCommManager.getLocalIPList();
		if(localAddressList == null) {
			System.err.println("Local address not found!");
			return;
		}
		String strCurrentLocalAddress = localAddressList.get(0).toString();

		// set config home
		m_clientStub.setConfigurationHome(Paths.get("."));
		// set file-path home
		m_clientStub.setTransferedFileHome(m_clientStub.getConfigurationHome().resolve("client-file-path"));

		// get the saved server info from the client configuration file
		String strSavedServerAddress = null;
		int nSavedServerPort = -1;

		strSavedServerAddress = m_clientStub.getServerAddress();
		nSavedServerPort = m_clientStub.getServerPort();

		// ask the user if he/she would like to change the server info
		JTextField currentLocalAddressTextField = new JTextField(strCurrentLocalAddress);
		currentLocalAddressTextField.setEnabled(false);
		JTextField serverAddressTextField = new JTextField(strSavedServerAddress);
		JTextField serverPortTextField = new JTextField(String.valueOf(nSavedServerPort));
		Object msg[] = {
				"My Current Address:", currentLocalAddressTextField,
				"Server Address:", serverAddressTextField,
				"Server Port:", serverPortTextField
		};
		int option = JOptionPane.showConfirmDialog(null, msg, "Server Information", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);

		// update the server info if the user would like to do
		if (option == JOptionPane.OK_OPTION)
		{
			String strNewServerAddress = serverAddressTextField.getText();
			int nNewServerPort = Integer.parseInt(serverPortTextField.getText());
			if(!strNewServerAddress.equals(strSavedServerAddress) || nNewServerPort != nSavedServerPort)
				m_clientStub.setServerInfo(strNewServerAddress, nNewServerPort);
		}

		bRet = m_clientStub.startCM();
		if(!bRet)
		{
			printStyledMessage("CM initialization error!\n", "bold");
		}
		else
		{
			m_startStopButton.setEnabled(true);
			m_loginLogoutButton.setEnabled(true);
			printStyledMessage("Client CM starts.\n", "bold");
			printStyledMessage("Type \"0\" for menu.\n", "regular");
			// change the appearance of buttons in the client window frame
			setButtonsAccordingToClientState();
		}
	}
	private void testChat()
	{
		String strTarget = null;
		String strMessage = null;

		printMessage("====== chat\n");

		JTextField targetField = new JTextField();
		JTextField messageField = new JTextField();
		Object[] message = {
				"Target(/b, /s, /g, or /username): ", targetField,
				"Message: ", messageField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Chat Input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(option == JOptionPane.OK_OPTION)
		{
			strTarget = targetField.getText();
			strMessage = messageField.getText();
			m_clientStub.chat(strTarget, strMessage);
		}

		printMessage("======\n");
	}

	private void testPushFile()
	{
		String strFilePath = null;
		File[] files = null;
		String strReceiver = null;
		byte byteFileAppendMode = -1;
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		boolean bReturn = false;

		printMessage("====== push a file\n");

		/*
		strReceiver = JOptionPane.showInputDialog("Receiver Name: ");
		if(strReceiver == null) return;
		*/
		JTextField freceiverField = new JTextField();
		String[] fAppendMode = {"Default", "Overwrite", "Append"};
		JComboBox<String> fAppendBox = new JComboBox<String>(fAppendMode);

		Object[] message = {
				"File Receiver(empty for default server): ", freceiverField,
				"File Append Mode: ", fAppendBox
				};
		int option = JOptionPane.showConfirmDialog(null, message, "File Push", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(option == JOptionPane.CANCEL_OPTION || option != JOptionPane.OK_OPTION)
		{
			printMessage("canceled.\n");
			return;
		}

		strReceiver = freceiverField.getText().trim();
		if(strReceiver.isEmpty())
			strReceiver = interInfo.getDefaultServerInfo().getServerName();

		switch(fAppendBox.getSelectedIndex())
		{
		case 0:
			byteFileAppendMode = CMInfo.FILE_DEFAULT;
			break;
		case 1:
			byteFileAppendMode = CMInfo.FILE_OVERWRITE;
			break;
		case 2:
			byteFileAppendMode = CMInfo.FILE_APPEND;
			break;
		}

		JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		File curDir = new File(confInfo.getTransferedFileHome().toString());
		fc.setCurrentDirectory(curDir);
		int fcRet = fc.showOpenDialog(this);
		if(fcRet != JFileChooser.APPROVE_OPTION) return;
		files = fc.getSelectedFiles();
		if(files.length < 1) return;
		for(int i=0; i < files.length; i++)
		{
			strFilePath = files[i].getPath();
			bReturn = m_clientStub.pushFile(strFilePath, strReceiver, byteFileAppendMode);
			if(!bReturn)
			{
				printMessage("push file error! file("+strFilePath+"), receiver("
						+strReceiver+")\n");
			}
		}

		printMessage("======\n");
	}


//	private void requestAttachedFile(String strFileName)
//	{
//		boolean bRet = m_clientStub.requestAttachedFileOfSNSContent(strFileName);
//		if(bRet)
//			m_eventHandler.setReqAttachedFile(true);
//		else
//			printMessage(strFileName+" not found in the downloaded content list!\n");
//
//		return;
//	}

//	private void accessAttachedFile(String strFileName)
//	{
//		boolean bRet = m_clientStub.accessAttachedFileOfSNSContent(strFileName);
//		if(!bRet)
//			printMessage(strFileName+" not found in the downloaded content list!\n");
//
//		return;
//	}

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
				try {
					processInput(strText);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
				input.setText("");
				input.requestFocus();
			}
			else if(key == KeyEvent.VK_ALT)
			{

			}
		}

		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
	}

	public class MyActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			JButton button = (JButton) e.getSource();
			if(button.getText().equals("Start Client CM"))
			{
				testStartCM();
			}
			else if(button.getText().equals("Stop Client CM"))
			{
				testTerminateCM();
			}
			else if(button.getText().equals("Login"))
			{
				// login to the default cm server
				testLoginDS();
			}
			else if(button.getText().equals("Logout"))
			{
				// logout from the default cm server
				testLogoutDS();
			}

			m_inTextField.requestFocus();
		}
	}

	public class MyMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String strMenu = e.getActionCommand();
			switch (strMenu) {
				case "show all menus":
					printAllMenus();
					break;
				case "current directory check":
					printMessage(getFilesInClientDirectory(m_clientStub).toString());
					printMessage("\n");
					break;
				case  "create file":
					Createfile(m_clientStub);
					break;
				case  "delete file":
					DeleteFile(m_clientStub);
					break;
				case "modify file":
					try {
						ModifyFile(m_clientStub);
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
					break;
				case "share":
					ShareFile(m_clientStub);
					break;

			}
		}
	}

	public class MyMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {  // 이게 뭘까?

			if(e.getSource() instanceof JLabel)
			{
				JLabel pathLabel = (JLabel)e.getSource();
				String strPath = pathLabel.getText();
				File fPath = new File(strPath);
				try {
					int index = strPath.lastIndexOf(File.separator);
					String strFileName = strPath.substring(index+1, strPath.length());
					if(fPath.exists())
					{
//						accessAttachedFile(strFileName);
						Desktop.getDesktop().open(fPath);
					}
					else
					{
//						requestAttachedFile(strFileName);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO: currently not needed

		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO: currently not needed

		}

		@Override
		public void mouseEntered(MouseEvent e) {
			if(e.getSource() instanceof JLabel)
			{
				Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				setCursor(cursor);
			}

		}

		@Override
		public void mouseExited(MouseEvent e) {
			if(e.getSource() instanceof JLabel)
			{
				Cursor cursor = Cursor.getDefaultCursor();
				setCursor(cursor);
			}
		}

	}

//	private void printThreadInfo() {
//		String threadInfo = m_clientStub.getThreadInfo();
//		printMessage(threadInfo);
//	}


	public  void uploadMultipleFiles(CMClientStub clientStub) {
	String[] strFiles = null;
	boolean bRequestResult = false;
	String strFileList = null;
	int nFileNum = -1;
	String fileNum = null;
	String strTarget = null;
		printMessage("====== push to SERVER  one or multiple files\n");
		JTextField receiverNameField = new JTextField();
		receiverNameField.setText("SERVER");
		JTextField numberField = new JTextField();
		JTextField filenameField = new JTextField();
		Object[] message = {
				"Input receiver name:", receiverNameField,
				"Number of files:", numberField,
				"file names separated with space:",filenameField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "upload input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION) {
			strTarget = receiverNameField.getText();
			fileNum = numberField.getText();
			nFileNum = Integer.parseInt(fileNum);
			strFileList = filenameField.getText();
			strFileList.trim();
			strFiles = strFileList.split("\\s+");
			if (strFiles.length != nFileNum) {
				System.out.println("The number of files incorrect!");
				return;
			}
			for (int i = 0; i < nFileNum; i++) {
				bRequestResult=clientStub.pushFile("./client-file-path/" + strFiles[i], strTarget);
				clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
				if(bRequestResult){
					printMessage("파일 서버로 업로드 완료\n");
				}
				else {
					printStyledMessage("파일 업로드 실패\n","bold");
				}
			}
		}
//	try {
//		System.out.print("Input receiver name: ");
//		strTarget = br.readLine();
//		System.out.print("Number of files: ");
//		nFileNum = Integer.parseInt(br.readLine());
//		System.out.print("Input file names separated with space: ");
//		strFileList = br.readLine();
//
//	} catch (NumberFormatException e) {
//		e.printStackTrace();
//		return;
//	} catch (IOException e) {
//		e.printStackTrace();
//		return;
//	}
	return;
}
	public  void get_file_content(CMClientStub clientStub) {
		String strFileName = null;
		printMessage("====== get file content\n");
		JTextField FileNameField = new JTextField();
		Object[] message = {
				"File name:", FileNameField,
		};
		int option = JOptionPane.showConfirmDialog(null, message, "request input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION){

			strFileName = FileNameField.getText();
			String filepath = "./client-file-path/" + strUserName + "/" + strFileName;
			printMessage(getFileContent(filepath));
		}
	}
	public  void request(CMClientStub clientStub) {// 파일 요청 함수

		String strFileName = null;
		String strFileOwner = null;
		boolean a = false;
		printMessage("====== request a file\n");
//		try {
//			System.out.print("File name: ");
//			strFileName = br.readLine();
//			System.out.print("File owner(server name): ");
//			strFileOwner = br.readLine();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		JTextField FileNameField = new JTextField();
		JTextField OwnerField = new JTextField();
		Object[] message = {
				"File name:", FileNameField,
				"File owner: ", OwnerField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "request input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if (option == JOptionPane.OK_OPTION)
			{
				strFileName = FileNameField.getText();
				strFileOwner = OwnerField.getText();
				a=clientStub.requestFile(strFileName, strFileOwner);
				if(a){
					printMessage("요청 성공\n");
				}
				else {
					printStyledMessage("요청 실패\n","bold");
				}
			}
	}
	public  void ShareFile(CMClientStub clientStub) {
		String fileName = null;
		String userName = null;
		boolean a = false;
		JTextField FileNameField = new JTextField();
		JTextField UserNameField = new JTextField();
		Object[] message = {
				"File name:", FileNameField,
				"User name:", UserNameField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "share input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION) {
			//		if (!clientStub.getGroupMembers().isMember(userName)) {
			//			System.out.print("User name: ");
			//			userName = br.readLine();
			//		}  로그인 하지 않은 클라이언트라면 다시 입력 받음
			fileName = FileNameField.getText();
			userName = UserNameField.getText();

			//			String message = fileName + " share_request_to " + userName + " " + m_eventHandler.logicalClock;
			clientStub.chat("/SERVER", fileName + " share_request_to " + userName + " " + m_eventHandler.logicalClock);
			printMessage("공유 요청을 보냈습니다.\n");

			if (!(fileName.contains("_shared"))) {
				String sharedFileName = fileName + "_shared";

				// 클라이언트의 파일 이름 변경
				String clientFilePath = "./client-file-path/" + strUserName + "/" + fileName;
				String sharedClientFilePath = "./client-file-path/" + strUserName + "/" + sharedFileName;

				File clientFile = new File(clientFilePath);
				File sharedClientFile = new File(sharedClientFilePath);

				if (clientFile.renameTo(sharedClientFile)) {
					printMessage("File name changed successfully.\n");
				} else {
					printStyledMessage("Failed to change file name.\n","bold");
				}
			}

		}
	}
	private  void DeleteFile(CMClientStub clientStub) {
		List<String> clientFileList = getFilesInClientDirectory(clientStub);
		String message1 = null;
		String fileName = null;
		JTextField FileNameField = new JTextField();
		Object[] message = {
				"File name:", FileNameField,
		};
		int option = JOptionPane.showConfirmDialog(null, message, "delete input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION) {

			fileName = FileNameField.getText();

			if (!clientFileList.contains(fileName)) {
				printStyledMessage("그런 파일이 업습니다.","bold");

			}
			else if (fileName.contains("_shared")){
				message1 = "모든 "+ fileName + " 파일 삭제바랍니다. "+m_eventHandler.logicalClock;
				clientStub.chat("/SERVER", message1);
				// 서버에 공유 파일 모두 삭제 메세지 요청 요청후 서버의 모든 공유 파일이 사라지면 파일 동기화에 의해 클라이언트의 파일도 삭제된다.
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {// 비공유 파일의 경우 바로 삭제 후에 동기화에 의해 서버에서 삭제됨
				File file = new File("./client-file-path/" + strUserName + "/" + fileName);
				boolean isDeleted = file.delete();
				if (isDeleted) {
					printMessage("File deleted successfully.\n");
				} else {
					printStyledMessage("Failed to delete the file.\n","bold");
				}
			}
		}
	}
	public  void Createfile(CMClientStub clientStub) {
		String strFileName = null;
		String strFileContent = null;
		printMessage("====== create a file\n");
		boolean isValidFileName = false;
		JTextField FileNameField = new JTextField();
		JTextField ContentField = new JTextField();
		Object[] message = {
				"File Name:", FileNameField,
				"File Content:", ContentField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Create file Input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION)
		{
			strFileName=FileNameField.getText();
			strFileContent= ContentField.getText();

			if(strFileName.contains("_shared")){
				printStyledMessage("File name cannot contain '_shared'. Please enter a different file name.","bold");
			}
			else{
				// Create the directory if it doesn't exist
				String directoryPath = "./client-file-path/"+strUserName;;
				File directory = new File(directoryPath);
				if (!directory.exists()) {
					if (directory.mkdirs()) {

					} else {
						System.err.println("Failed to create directory: " + directoryPath);
						return;
					}
				}
				try {
					FileWriter fileWriter = new FileWriter(directoryPath + "/" + strFileName);
					fileWriter.write(strFileContent);
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				printMessage(strFileName+"file create success\n");
				clientStub.pushFile(directoryPath + "/" + strFileName, "SERVER");
				clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
			}
		}
	}
	private  void ModifyFile(CMClientStub clientStub) throws IOException {
		List<String> clientFileList = getFilesInClientDirectory(clientStub);
		String strFileName = null;
		String strFileContent = null;
		printMessage("====== modify a file\n");

		JTextField FileNameField = new JTextField();
		JTextField ContentField = new JTextField();
		Object[] message = {
				"File Name:", FileNameField,
				"File Content:", ContentField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Modify File Input", JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (option == JOptionPane.OK_OPTION)
		{
			strFileName=FileNameField.getText();
			strFileContent= ContentField.getText();

			if (!clientFileList.contains(strFileName)) {
				printStyledMessage("그런 파일이 없습니다.\n","bold");
			}
			else{
				FileWriter fileWriter = new FileWriter("./client-file-path/"+strUserName+"/"+strFileName);
				fileWriter.write(strFileContent);
				fileWriter.close();
				printMessage("File modified successfully.\n");
			}
		}
	}

private static List<String> getFilesInClientDirectory(CMClientStub clientStub) {
	List<String> fileList = new ArrayList<>();
	File directory = new File("./client-file-path/"+strUserName);
	if (directory.exists() && directory.isDirectory()) {
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					fileList.add(file.getName());
				}
			}
		}
	}
	return fileList;
}
	private static List<String> getFilesInServerDirectory(CMClientStub clientStub) {
		List<String> fileList = new ArrayList<>();
		File directory = new File("./server-file-path/"+strUserName);
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						fileList.add(file.getName());
					}
				}
			}
		}
		return fileList;
	}
	private  static  void updateSync(CMClientStub clientStub) {
		// 목록 가져오기
		List<String> clientFileList = getFilesInClientDirectory(clientStub);
		List<String> serverFileList = getFilesInServerDirectory(clientStub);

		// 클라이언트에만 존재하는 파일을 두가지로 처리 1.서버로 보내기 2. 클라이언트에서 삭제
		for (String clientFile : clientFileList) {
			if (!serverFileList.contains(clientFile)) {
				if(clientFile.contains("_shared")){// _shared 태그를 달고 있는 파일이 서버에는 없고 클라이언트에만 있다면
					// 다른 클라이언트에서 삭제 되어 서버의 모든 파일이 삭제 되었으므로 클라이언트에서 삭제. 이유(클라이언트가 로그아웃해 있을때 삭제 되었을 경우 세션에 들어왔을때 동기화 되게하기 위해
					File file = new File("./client-file-path/"+strUserName+"/"+clientFile);
					boolean isDeleted = file.delete();
					if (isDeleted) {
						System.out.println(clientFile + " File deleted successfully.");
					}
				}

				else {
					String filePath = "./client-file-path/" + strUserName + "/" + clientFile;
					clientStub.pushFile(filePath, "SERVER");
					clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
					try {
						// Wait for the specified duration
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}
	private  static void deleteSync(CMClientStub clientStub) {
		List<String> clientFileList = getFilesInClientDirectory(clientStub);
		List<String> serverFileList = getFilesInServerDirectory(clientStub);
		// 서버에만 존재하는 파일을 서버에 삭제 요청하기
		for (String serverFile : serverFileList) {
			if (!clientFileList.contains(serverFile)) {
				if(serverFile.contains("_shared")){
					String message = "모든 "+serverFile + " 파일 삭제바랍니다. "+m_eventHandler.logicalClock;
					clientStub.chat("/SERVER", message);// 서버에 공유 파일 모두 삭제 메세지 요청
					try {// 일단 슬립  logical lock 으로 구현해야함
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					String message = serverFile + " deleted. "+m_eventHandler.logicalClock;
					clientStub.chat("/SERVER", message);// 서버에 삭제 메세지 요청

					try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}


	// 서버 접근 말고 뭘로 바꾸지...
	private static  void compareSync(CMClientStub clientStub) {
		List<String> clientFileList = getFilesInClientDirectory(clientStub);
		// 파일 내용 비교 및 업데이트
		for (String fileName : clientFileList) {
			String clientFilePath = "./client-file-path/" + strUserName + "/" + fileName;
			String serverFilePath = "./server-file-path/" + strUserName + "/" + fileName;

			String clientFileContent = getFileContent(clientFilePath);
			String serverFileContent = getFileContent(serverFilePath);

			if (!clientFileContent.equals(serverFileContent)) {// 내용이 다른 경우
				if (fileName.contains("_shared")) {  // 공유된 파일이라면 서버 내의 모든 파일 업데이트
					String message1 = fileName + " deleted. " + m_eventHandler.logicalClock;
					clientStub.chat("/SERVER", message1);
					clientStub.pushFile(clientFilePath, "SERVER");// 재전송
					clientStub.chat("/SERVER", "file_send " + m_eventHandler.logicalClock);
					try {// 일단 슬립  logical lock 으로 구현해야함
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					String message2 = fileName + " 모두 업데이트 " + m_eventHandler.logicalClock;
					clientStub.chat("/SERVER", message2);
				} else {
					// 내용이 다르다면 파일 삭제 후 재전송
					String message = fileName + " deleted. "+m_eventHandler.logicalClock;
					clientStub.chat("/SERVER", message);

					clientStub.pushFile(clientFilePath, "SERVER");
					clientStub.chat("/SERVER","file_send "+m_eventHandler.logicalClock);
				}
			}
		}
	}
	private  static void synchronizeFilesWithServer(CMClientStub clientStub) { // 동기화 함수
		updateSync(clientStub);
		// 서버에만 존재하는 파일을 서버에 삭제 요청하기
		deleteSync(clientStub);

		compareSync(clientStub);
	}
	private  static String getFileContent(String filePath) {  // 파일 수정을 위해서 파일을 읽어오는 함수
		try {
			byte[] encodedBytes = Files.readAllBytes(Paths.get(filePath));
			return new String(encodedBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}



	public static void main(String[] args) {
		MyWinClient client = new MyWinClient();
		CMClientStub cmStub = client.getClientStub();
		cmStub.setAppEventHandler(client.getClientEventHandler());
		timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				synchronizeFilesWithServer(cmStub);
			}
		};
		//15초마다 실행
		timer.schedule(task, 5000, 15000);
	}

}
