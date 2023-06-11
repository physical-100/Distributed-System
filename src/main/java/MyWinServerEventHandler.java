import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class MyWinServerEventHandler implements CMAppEventHandler {
	private MyWinServer m_server;
	private CMServerStub m_serverStub;
	private int server_logicalClock;

	public MyWinServerEventHandler(CMServerStub serverStub, MyWinServer server) {
		m_server = server;
		server_logicalClock = 0;
		m_serverStub = serverStub;
	}

	@Override
	public void processEvent(CMEvent cme) {

		switch (cme.getType()) {
			case CMInfo.CM_SESSION_EVENT:
				server_logicalClock++;
				SprocessSessionEvent(cme);
				break;
			case CMInfo.CM_INTEREST_EVENT:
				server_logicalClock++;
				SprocessInterestEvent(cme);
				break;

			case CMInfo.CM_FILE_EVENT:
				server_logicalClock++;
				SprocessFileEvent(cme);
				break;
			default:
				return;
		}
	}

	private void SprocessSessionEvent(CMEvent cme) {
		CMSessionEvent se = (CMSessionEvent) cme;
		switch (se.getID()) {
			case CMSessionEvent.LOGIN:
				String userName = se.getUserName();
				printMessage("[" + userName + "] login.\n");

				String directoryPath = "./server-file-path/" + userName;
				File directory = new File(directoryPath);

				if (!directory.exists()) {
					if (directory.mkdirs()) {
						// 디렉토리 만들기
					} else {
						printMessage("Failed to create directory: " + directoryPath+"\n");
					}
				}
				break;

			case CMSessionEvent.LOGOUT:
				printMessage("[" + se.getUserName() + "] logout this server.\n");
				//로그 아웃시 서버에 존재하는 파일 삭제
				String logoutdirectoryPath = "./server-file-path/" + se.getUserName();
				File directory1 = new File(logoutdirectoryPath);

				if (directory1.isDirectory()) {
					File[] files = directory1.listFiles();
					if (files != null) {
						for (File file : files) {
							if (file.isFile()) {
								if (file.toString().contains("_shared"))
									file.delete(); // 파일 삭제
							}
						}
					}
//                    printMessage("공유된 파일 삭제");
				}
				break;
			default:
				return;
		}

	}

	private void SprocessInterestEvent(CMEvent cme) {
		// chat메세지가 올때마다 logical clock 값을 받아서 업데이트

		CMInterestEvent ie = (CMInterestEvent) cme;
		switch (ie.getID()) {
			case CMInterestEvent.USER_TALK:
				printMessage("<" + ie.getUserName() + ">: " + ie.getTalk()+"\n");
				getLogicalClocks(ie.getTalk());
				if (ie.getTalk().contains("모든")) {  // 공유된 파일 삭제 시  서버의 모든 파일 삭제
					String[] parts = ie.getTalk().split("\\s+");
					String filename = parts[1];
					for (String DirName : getDirectoryPathsWithFile("./server-file-path", filename)) {
						String extractedString = DirName.substring(DirName.lastIndexOf("/") + 1); // 서버에 파일 이름을 가진 디렉토르 추출
						File file = new File("./server-file-path/" + extractedString + "/" + filename);
						boolean isDeleted = file.delete();
						if (isDeleted) {
							printMessage(extractedString+"/"+filename + " File deleted successfully.\n");
						} else {
							System.err.println("Failed to delete the file.");
						}
						// 서버에서 삭제 이후 클라이언트에 있는 파일은 동기화에 의해 삭제됨
					}
				} else if (ie.getTalk().contains("모두 업데이트")) {// 파일 업데이트 후 클라이언트에게 전송
					String[] parts = ie.getTalk().split("\\s+");
					String filename = parts[0];
					if (!getDirectoryPathsWithFile("./server-file-path", filename).isEmpty()) {
						for (String DirName : getDirectoryPathsWithFile("./server-file-path", filename)) {// 서버에 파일 이름을 가진 디렉토리 추출
							String extractedString = DirName.substring(DirName.lastIndexOf("/") + 1);
							printMessage(extractedString+"\n");
							if (!extractedString.equals(ie.getUserName())) {
								String sourcePath = "./server-file-path/" + ie.getUserName() + "/" + filename;
								String destinationPath = "./server-file-path/" + extractedString + "/" + filename;

								Path source = Paths.get(sourcePath);
								Path destination = Paths.get(destinationPath);

								try {
									Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
									printMessage("File move successfully.\n");
									m_serverStub.pushFile("./server-file-path/" + extractedString + "/" + filename, extractedString);
									send_dummyevent(server_logicalClock + " logicalclock_change", extractedString);
								} catch (IOException e) {
									e.printStackTrace();
									printMessage("Failed to move the file.\n");
								}
							}
						}
					}
					else {
						printMessage("파일이 수정되기전 모두 삭제되었습니다.\n");
						send_dummyevent(server_logicalClock+" 파일이_수정되기전_모두_삭제되었습니다.", ie.getUserName());
					}
				} else if (ie.getTalk().contains("deleted")) {// 삭제 채팅을 받았을 때 처리
					String[] parts = ie.getTalk().split("\\s+"); // Split the string by whitespace
					String filename = parts[0];
					File file = new File("./server-file-path/" + ie.getUserName() + "/" + filename);
					boolean isDeleted = file.delete();
					if (isDeleted) {
						printMessage(filename + " File deleted successfully.\n");

					} else {
						printMessage("Failed to delete the file.\n");
					}
				} else if (ie.getTalk().contains("share_request_to")) {
					String[] parts = ie.getTalk().split("\\s+"); // Split the string by whitespace
					String filename = parts[0];
					String username = parts[2];

					if ((filename.contains("_shared"))) { //이미 공유되었던 이름이라면 변경하지 않고 클라이언트로 전송해줌

						//큐에 넣고 대기 해야 하는 것
//                        messageQueue.add("<" + ie.getUserName() + ">: " + ie.getTalk()); // 예시 메시지, 필요에 따라 변경
						m_serverStub.pushFile("./server-file-path/" + ie.getUserName() + "/" + filename, username);
						send_dummyevent(server_logicalClock+ " logicalclock_change",username);
						// push와 동시에 username 디렉토리 생성 후 파일 복사 해옴
						String destinationPath = "./server-file-path/" + username + "/" + filename;
						Path source = Paths.get("./server-file-path/" + ie.getUserName() + "/" + filename);
						Path destination = Paths.get(destinationPath);

						try {
							// 대상 파일이 존재하지 않으면 생성
							if (!Files.exists(destination)) {
								Files.createFile(destination);
							}

							// 파일 복사능
							Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
							printMessage( username +" 에 파일이 성공적으로 생성되었습니다.\n");
						} catch (IOException e) {
							e.printStackTrace();
							printMessage("파일 생성에 실패하였습니다.\n");
						}
					}
					else {
						// 서버에 저장되어있던 클라이언트의 이름 변경
						// 이름으로 비교를 하면 이름이 같은데 공유되지 않은 파일도 동기화 되는것을 막아주기 위해 _shared를 붙인 파일이름으로 바꾸어준다.
						String sharedFileName = filename + "_shared";
						String ServerFilePath = "./server-file-path/" + ie.getUserName() + "/" + filename;
						String sharedServerFilePath = "./server-file-path/" + ie.getUserName() + "/" + sharedFileName;

						File clientFile = new File(ServerFilePath);
						File sharedClientFile = new File(sharedServerFilePath);

						if (clientFile.renameTo(sharedClientFile)) {
							printMessage("File name changed successfully.\n");
							m_serverStub.pushFile(sharedServerFilePath, username);
							send_dummyevent(server_logicalClock+ " logicalclock_change",username);

						} else {
							printMessage("Failed to change file name.\n");
						}
						//서버에도 파일 복사해서 생성
						String destinationPath = "./server-file-path/" + username + "/" + sharedFileName;
						Path source = Paths.get("./server-file-path/" + ie.getUserName() + "/" + sharedFileName);
						Path destination = Paths.get(destinationPath);

						try {
							// 대상 파일이 존재하지 않으면 생성
							if (!Files.exists(destination)) {
								Files.createFile(destination);
							}

							// 파일 복사
							Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
							printMessage("파일이 성공적으로 생성되었습니다.\n");
						} catch (IOException e) {
							e.printStackTrace();
							printMessage("파일 생성에 실패하였습니다.\n");
						}

					}
				} else if (ie.getTalk().contains("file_send")) {
					send_dummyevent(server_logicalClock+ " logicalclock_change", ie.getUserName());
				}else if (ie.getTalk().contains("파일 동기화 요청")) {
					String[] parts = ie.getTalk().split("\\s+"); // Split the string by whitespace
					String filename = parts[0];
					send_dummyevent(server_logicalClock + "동기화 가능", ie.getUserName());
//                    }else {
//                        messageQueue.add(ie.getTalk());
//                    }
				}

				break;
			default:
				return;
		}
	}

	private void SprocessFileEvent(CMEvent cme) {
		CMFileEvent fe = (CMFileEvent) cme;

		switch (fe.getID()) {
			case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
				printMessage("[" + fe.getFileReceiver() + "] requests file(" + fe.getFileName() + ").\n");
			case CMFileEvent.REPLY_PERMIT_PULL_FILE:
				if (fe.getReturnCode() == -1) {
					System.err.print("[" + fe.getFileName() + "] does not exist in the owner!\n");
				} else if (fe.getReturnCode() == 0) {
					System.err.print("[" + fe.getFileSender() + "] rejects to send file(" + fe.getFileName() + ").\n");
				} else
					printMessage("[" + fe.getFileSender() + "] send file(" + fe.getFileName() + ").\n");
				break;
			case CMFileEvent.END_FILE_TRANSFER:
				printMessage(fe.getFileName() + " 파일 수신이 완료되었습니다.\n");
				break;
			case CMFileEvent.END_FILE_TRANSFER_ACK:
				if (fe.getReturnCode() == 1) {
					System.out.print(" transfer success\n");
					if (fe.getFileSender().equals("SERVER")) {
						if (fe.getFileName().contains("_shared")) {
							send_dummyevent(fe.getFileName()+" server file send",fe.getFileReceiver());
						}
					}
				} else if (fe.getReturnCode() == 0) {
					System.err.print("[" + fe.getFileReceiver() + "] receive fail\n");
					break;
				}

				return;
		}
	}
	private  void send_dummyevent(String msg,String reciever){
		CMDummyEvent due = new CMDummyEvent();
		due.setDummyInfo(msg);
		m_serverStub.send(due, reciever);
		due = null;
	}
	private void getLogicalClocks(String getString) {
		String[] msg = getString.split("\\s+");
		String lastElement = msg[msg.length - 1];
		int clientLogicalClock = Integer.parseInt(lastElement);
		//서버 로지컬 클락 세팅
		server_logicalClock=compareLogicalClocks(server_logicalClock,clientLogicalClock);
	}
	private int compareLogicalClocks(int serverLogicalClock, int clientLogicalClock) {// logical clock 비교 해서 더  큰 값 보다 +1 해서 내놓음
		if (serverLogicalClock >= clientLogicalClock) {
			return serverLogicalClock+1;
		} else {
			return clientLogicalClock+1;
		}
	}
	public static List<String> getDirectoryPathsWithFile(String directoryPath, String filename) {
		// filename을 가진 디렉토리 모두 반환
		List<String> directoryPaths = new ArrayList<>();
		File directory = new File(directoryPath);
		searchDirectory(directory, filename, directoryPaths);
		return directoryPaths;
	}

	private static void searchDirectory(File directory, String filename, List<String> directoryPaths) {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				boolean hasFile = false;
				for (File file : files) {
					if (file.isFile() && file.getName().equals(filename)) {
						hasFile = true;
						break;
					} else if (file.isDirectory()) {
						searchDirectory(file, filename, directoryPaths);
					}
				}
				if (hasFile) {
					directoryPaths.add(directory.getAbsolutePath());
				}
			}
		}
	}
private void printMessage(String strText)
{
	m_server.printMessage(strText);
}

}
