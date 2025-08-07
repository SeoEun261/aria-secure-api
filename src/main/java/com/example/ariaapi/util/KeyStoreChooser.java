package com.example.ariaapi.util;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * KeyStore 파일 선택 및 비밀번호 입력 도구.
 * <p>
 * 이 도구는 키스토어 파일을 선택하고 비밀번호를 입력받아
 * application.properties 파일을 자동으로 업데이트하는 기능을 제공합니다.
 * 기존 파일 내용을 유지하고 특정 설정만 수정하는 방식으로 동작하여 깔끔합니다.
 * </p>
 * <p>
 * 사용 방법:
 * 1. 이 클래스의 main() 메소드를 실행합니다.
 * 2. 팝업된 파일 선택 창에서 키스토어(.p12 또는 .jks) 파일을 선택합니다.
 * 3. 콘솔에 표시되는 안내에 따라 비밀번호와 키 별칭을 입력합니다.
 * 4. 입력된 정보로 application.properties 파일이 자동으로 업데이트됩니다.
 * </p>
 */
public class KeyStoreChooser {

    private static final String APP_PROPERTIES_PATH = "src/main/resources/application.properties";
    private static final String TEST_PROPERTIES_PATH = "src/test/resources/application.properties";
    private static final String[] KEYSTORE_PROPS = {
            "app.keystore.location",
            "app.keystore.password",
            "app.keystore.key-alias",
            "app.keystore.key-password"
    };

    public static void main(String[] args) {
        // Swing UI는 Event Dispatch Thread에서 실행되어야 하므로 SwingUtilities.invokeLater를 사용합니다.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Failed to set native look and feel. Continuing with default.");
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("KeyStore 파일을 선택하세요");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // 현재 작업 디렉토리에서 시작
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();

                System.out.println("---------------------------------------------");
                System.out.println("✅ KeyStore 파일이 성공적으로 선택되었습니다.");
                System.out.println("  -> 경로: " + filePath);
                System.out.println("---------------------------------------------");

                try (Scanner scanner = new Scanner(System.in)) {
                    System.out.print("1. KeyStore 파일 비밀번호를 입력하세요: ");
                    String keyStorePassword = scanner.nextLine();

                    System.out.print("2. Key의 별칭(Alias)을 입력하세요: ");
                    String keyAlias = scanner.nextLine();

                    System.out.print("3. Key 비밀번호를 입력하세요: ");
                    String keyPassword = scanner.nextLine();

                    // 입력된 정보로 application.properties 파일들을 업데이트
                    updatePropertiesFile(APP_PROPERTIES_PATH, filePath, keyStorePassword, keyAlias, keyPassword);
                    updatePropertiesFile(TEST_PROPERTIES_PATH, filePath, keyStorePassword, keyAlias, keyPassword);

                    System.out.println("\n🎉 application.properties 파일이 성공적으로 업데이트되었습니다. 이제 메인 애플리케이션을 실행하세요.");
                }

            } else {
                System.out.println("파일 선택이 취소되었습니다. 프로그램을 종료합니다.");
            }
        });
    }

    /**
     * 지정된 경로의 properties 파일을 읽고, KeyStore 관련 설정을 업데이트한 후 저장합니다.
     * 기존의 다른 내용은 유지됩니다.
     * @param propertiesFilePath 업데이트할 파일 경로
     * @param filePath KeyStore 파일의 절대 경로
     * @param keyStorePassword KeyStore 비밀번호
     * @param keyAlias Key 별칭
     * @param keyPassword Key 비밀번호
     */
    private static void updatePropertiesFile(String propertiesFilePath, String filePath, String keyStorePassword, String keyAlias, String keyPassword) {
        Path path = Paths.get(propertiesFilePath);

        try {
            // 파일이 존재하지 않으면 오류 메시지 출력
            if (!Files.exists(path)) {
                System.err.println("🚨 파일이 존재하지 않아 업데이트할 수 없습니다: " + propertiesFilePath);
                return;
            }

            List<String> lines = Files.lines(path).collect(Collectors.toList());
            boolean[] propsUpdated = new boolean[KEYSTORE_PROPS.length];

            // 파일 경로에 슬래시를 일관되게 적용
            String formattedPath = "file:" + filePath.replace("\\", "/");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                // 기존 KeyStore 설정이 있는지 확인하고 업데이트
                for (int j = 0; j < KEYSTORE_PROPS.length; j++) {
                    if (line.trim().startsWith(KEYSTORE_PROPS[j] + "=")) {
                        switch (KEYSTORE_PROPS[j]) {
                            case "app.keystore.location":
                                lines.set(i, KEYSTORE_PROPS[j] + "=" + formattedPath);
                                break;
                            case "app.keystore.password":
                                lines.set(i, KEYSTORE_PROPS[j] + "=" + keyStorePassword);
                                break;
                            case "app.keystore.key-alias":
                                lines.set(i, KEYSTORE_PROPS[j] + "=" + keyAlias);
                                break;
                            case "app.keystore.key-password":
                                lines.set(i, KEYSTORE_PROPS[j] + "=" + keyPassword);
                                break;
                        }
                        propsUpdated[j] = true;
                    }
                }
            }

            // 기존에 설정이 없었으면 파일 맨 끝에 추가
            lines.add(""); // 줄바꿈 추가
            lines.add("# KeyStore settings updated by KeyStoreChooser tool");
            for (int i = 0; i < KEYSTORE_PROPS.length; i++) {
                if (!propsUpdated[i]) {
                    switch (KEYSTORE_PROPS[i]) {
                        case "app.keystore.location":
                            lines.add(KEYSTORE_PROPS[i] + "=" + formattedPath);
                            break;
                        case "app.keystore.password":
                            lines.add(KEYSTORE_PROPS[i] + "=" + keyStorePassword);
                            break;
                        case "app.keystore.key-alias":
                            lines.add(KEYSTORE_PROPS[i] + "=" + keyAlias);
                            break;
                        case "app.keystore.key-password":
                            lines.add(KEYSTORE_PROPS[i] + "=" + keyPassword);
                            break;
                    }
                }
            }

            // 수정된 내용을 파일에 다시 쓰기
            Files.write(path, lines);

            System.out.println("✅ " + propertiesFilePath + " 파일이 업데이트되었습니다.");

        } catch (IOException e) {
            System.err.println("파일 업데이트 중 오류가 발생했습니다: " + propertiesFilePath);
            e.printStackTrace();
        }
    }
}
