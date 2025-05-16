import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySQLConnectionTest extends JFrame {
    // UI 컴포넌트
    private JTextField hostField;
    private JTextField portField;
    private JTextField dbNameField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JTextArea resultArea;
    private JButton connectButton;
    private JButton startServerButton;
    private JTextField mysqlPathField;
    
    // JDBC 드라이버
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    // 스레드 관리를 위한 ExecutorService
    private ExecutorService executorService;

    public MySQLConnectionTest() {
        // 스레드 풀 초기화
        executorService = Executors.newFixedThreadPool(2);
        
        // UI 초기화
        initUI();
    }
    
    private void initUI() {
        setTitle("MySQL 연결 테스트");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 레이아웃 설정
        setLayout(new BorderLayout());
        
        // 입력 패널
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        inputPanel.add(new JLabel("MySQL 설치 경로:"));
        mysqlPathField = new JTextField(getDefaultMySQLPath());
        inputPanel.add(mysqlPathField);
        
        inputPanel.add(new JLabel("호스트:"));
        hostField = new JTextField("localhost");
        inputPanel.add(hostField);
        
        inputPanel.add(new JLabel("포트:"));
        portField = new JTextField("3306");
        inputPanel.add(portField);
        
        inputPanel.add(new JLabel("데이터베이스 이름:"));
        dbNameField = new JTextField("test_db");
        inputPanel.add(dbNameField);
        
        inputPanel.add(new JLabel("사용자:"));
        userField = new JTextField("root");
        inputPanel.add(userField);
        
        inputPanel.add(new JLabel("비밀번호:"));
        passwordField = new JPasswordField();
        inputPanel.add(passwordField);
        
        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        
        startServerButton = new JButton("MySQL 서버 시작");
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startMySQLServer();
            }
        });
        buttonPanel.add(startServerButton);
        
        connectButton = new JButton("연결 테스트");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });
        buttonPanel.add(connectButton);
        
        // 결과 영역
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(550, 300));
        
        // 컴포넌트 배치
        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
        
        // 윈도우 종료 시 스레드 풀 종료
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executorService.shutdown();
            }
        });
    }
    
    private String getDefaultMySQLPath() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\";
        } else if (os.contains("mac")) {
            return "/usr/local/mysql/bin/";
        } else { // Linux
            return "/usr/bin/";
        }
    }
    
    private void startMySQLServer() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String mysqlPath = mysqlPathField.getText().trim();
                    String os = System.getProperty("os.name").toLowerCase();
                    ProcessBuilder processBuilder;
                    
                    appendToResults("MySQL 서버 시작 중...\n");
                    
                    if (os.contains("win")) {
                        // Windows
                        processBuilder = new ProcessBuilder(
                            "cmd.exe", "/c", 
                            "net", "start", "MySQL80"
                        );
                    } else if (os.contains("mac")) {
                        // macOS
                        processBuilder = new ProcessBuilder(
                            "sudo", 
                            "launchctl", "load", "-F", 
                            "/Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist"
                        );
                    } else {
                        // Linux
                        processBuilder = new ProcessBuilder(
                            "sudo", 
                            "systemctl", "start", "mysql"
                        );
                    }
                    
                    Process process = processBuilder.start();
                    
                    // 프로세스 출력 읽기
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                appendToResults(outputLine + "\n");
                            }
                        });
                    }
                    
                    // 프로세스 에러 읽기
                    reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                    
                    while ((line = reader.readLine()) != null) {
                        final String errorLine = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                appendToResults("오류: " + errorLine + "\n");
                            }
                        });
                    }
                    
                    int exitCode = process.waitFor();
                    final int exitVal = exitCode;
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (exitVal == 0) {
                                appendToResults("MySQL 서버가 성공적으로 시작되었습니다.\n");
                            } else {
                                appendToResults("MySQL 서버 시작 실패! 종료 코드: " + exitVal + "\n");
                                appendToResults("서버가 이미 실행 중이거나 관리자 권한이 필요할 수 있습니다.\n");
                            }
                        }
                    });
                    
                } catch (IOException | InterruptedException e) {
                    final String errorMsg = e.getMessage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            appendToResults("서버 시작 중 오류 발생: " + errorMsg + "\n");
                        }
                    });
                }
            }
        });
    }
    
    private void testConnection() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // 사용자 입력 가져오기
                String host = hostField.getText().trim();
                String port = portField.getText().trim();
                String dbName = dbNameField.getText().trim();
                String user = userField.getText().trim();
                String password = new String(passwordField.getPassword());
                
                // URL 생성
                String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + 
                                "?useSSL=false&serverTimezone=UTC";
                
                // 결과 영역 초기화
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        appendToResults("MySQL 서버에 연결 시도 중...\n");
                    }
                });
                
                // 커넥션 초기화
                Connection conn = null;
                
                try {
                    // JDBC 드라이버 로딩
                    Class.forName(JDBC_DRIVER);
                    final String driverName = JDBC_DRIVER;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            appendToResults("JDBC 드라이버 로딩 성공: " + driverName + "\n");
                        }
                    });
                    
                    // 연결 시작 시간 기록
                    long startTime = System.currentTimeMillis();
                    
                    // 데이터베이스 연결
                    conn = DriverManager.getConnection(dbUrl, user, password);
                    
                    // 연결 완료 시간 계산
                    long endTime = System.currentTimeMillis();
                    long connectionTime = endTime - startTime;
                    
                    // 연결 확인
                    if (conn != null) {
                        final Connection finalConn = conn;
                        final long finalTime = connectionTime;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    appendToResults("연결 성공! 연결 시간: " + finalTime + "ms\n");
                                    appendToResults("데이터베이스 제품명: " + finalConn.getMetaData().getDatabaseProductName() + "\n");
                                    appendToResults("데이터베이스 제품 버전: " + finalConn.getMetaData().getDatabaseProductVersion() + "\n");
                                    appendToResults("데이터베이스 URL: " + finalConn.getMetaData().getURL() + "\n");
                                    appendToResults("JDBC 드라이버 이름: " + finalConn.getMetaData().getDriverName() + "\n");
                                    appendToResults("JDBC 드라이버 버전: " + finalConn.getMetaData().getDriverVersion() + "\n");
                                } catch (SQLException e) {
                                    appendToResults("메타데이터 조회 중 오류: " + e.getMessage() + "\n");
                                }
                            }
                        });
                    }
                    
                } catch (final SQLException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            appendToResults("MySQL 연결 실패!\n");
                            appendToResults("SQLException: " + e.getMessage() + "\n");
                            appendToResults("SQLState: " + e.getSQLState() + "\n");
                            appendToResults("VendorError: " + e.getErrorCode() + "\n");
                            
                            // 서버가 실행 중이지 않은 경우 안내 메시지 표시
                            if (e.getMessage().contains("Connection refused") || 
                                e.getMessage().contains("Communications link failure")) {
                                appendToResults("\nMySQL 서버가 실행 중이지 않은 것 같습니다.\n");
                                appendToResults("'MySQL 서버 시작' 버튼을 클릭하여 서버를 시작해보세요.\n");
                            }
                        }
                    });
                } catch (final ClassNotFoundException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            appendToResults("JDBC 드라이버를 찾을 수 없습니다: " + e.getMessage() + "\n");
                        }
                    });
                } finally {
                    // 연결 종료
                    try {
                        if (conn != null) {
                            conn.close();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    appendToResults("데이터베이스 연결이 종료되었습니다.\n");
                                }
                            });
                        }
                    } catch (final SQLException e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                appendToResults("연결 종료 중 오류 발생: " + e.getMessage() + "\n");
                            }
                        });
                    }
                }
            }
        });
    }
    
    private void appendToResults(String text) {
        resultArea.append(text);
        // 자동 스크롤
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    }
    
    // 서버 상태 확인 메서드 추가 
    private boolean isServerRunning() {
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String user = userField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // 테스트용 URL (mysql 데이터베이스는 항상 존재)
        String testUrl = "jdbc:mysql://" + host + ":" + port + 
                          "/mysql?useSSL=false&serverTimezone=UTC&connectTimeout=1000";
        
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(testUrl, user, password);
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 무시
                }
            }
        }
    }

    public static void main(String[] args) {
        // UI 스레드에서 실행
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MySQLConnectionTest app = new MySQLConnectionTest();
                app.setVisible(true);
            }
        });
    }
}
