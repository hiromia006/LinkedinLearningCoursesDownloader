package linkedin.com.downloader;

import com.utils.ConfigurationManager;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Linkedin_Learning_Courses_Downloader {
    static WebDriver driver = null;
    static String rootPth = "";
    static String projectHomeDirectory = "";
    static String courseTittle = "";

    public static void main(String[] args) throws IOException {
        ConfigurationManager configurationManager = new ConfigurationManager();

        projectHomeDirectory = System.getProperty("user.dir");
        FileReader fr = new FileReader(projectHomeDirectory + "/src/test/resources/coursesUrl.txt");
        BufferedReader br = new BufferedReader(fr);
        String st;
        while ((st = br.readLine()) != null) {
            try {
                String osName = System.getProperty("os.name");
                System.out.println("OS Name : " + osName);
                System.out.println("project Home Directory :: " + projectHomeDirectory);

                if (osName.equalsIgnoreCase("Linux")) {
                    System.setProperty("webdriver.gecko.driver", projectHomeDirectory + "/drivers/geckodriver");
                } else {
                    System.setProperty("webdriver.gecko.driver", projectHomeDirectory + "/drivers/geckodriver.exe");
                }

                if (Objects.equals(configurationManager.getBrowser(), "headless")) {
                    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
                    FirefoxBinary firefoxBinary = new FirefoxBinary();
                    FirefoxOptions options = new FirefoxOptions();
                    options.setBinary(firefoxBinary);
                    options.setHeadless(true);  // <-- headless set here
                    options.setAcceptInsecureCerts(true);
                    driver = new FirefoxDriver(options);
                } else {
                    driver = new FirefoxDriver();
                }

                driver.get(configurationManager.getBaseUrl());
                driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
                Thread.sleep(1000);

                WebElement fillEmail = driver.findElement(By.cssSelector("input#auth-id-input"));
                fillEmail.clear();
                fillEmail.sendKeys(configurationManager.getEmail());

                WebElement continueBtn = driver.findElement(By.cssSelector("button#auth-id-button"));
                continueBtn.click();
                Thread.sleep(500);

                WebElement fillPassword = driver.findElement(By.cssSelector("input#password"));
                fillPassword.clear();
                fillPassword.sendKeys(configurationManager.getPassword());
                Thread.sleep(500);

                WebElement loginBtn = driver.findElement(By.cssSelector("div[class*='login__form_action_container'] button"));
                Thread.sleep(1000);
                loginBtn.click();
                driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
                customWait();

                String url = st.trim();
                courseTittle = url.split("/")[4].trim().replaceAll("-", " ");
                System.out.println(url);
                rootPth = projectHomeDirectory + "/videos/" + courseTittle;
                File theDir = new File(rootPth);
                if (!theDir.exists()) {
                    theDir.mkdirs();
                }


                driver.get(url);
                driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
                Thread.sleep(5000);
                customWait();
                waitUntilVideoLoaded();

                createDocxFile();
                appendTextIntoDocx("Course details", 15);
                Thread.sleep(500);
                String courseDetails = driver.findElement(By.cssSelector("div.classroom-layout-panel-layout-main  p[class]")).getText().trim();
                appendTextIntoDocx(courseDetails, 12);

                appendTextIntoDocx("Learning objectives", 15);
                List<WebElement> objectives = driver.findElements(By.cssSelector("ul.classroom-workspace-overview__learning-objective-list  > li"));
                for (WebElement webElement : objectives) {
                    appendTextIntoDocx(webElement.getText().trim(), 12);
                }

                List<WebElement> sections = null;
                sections = driver.findElements(By.cssSelector("section.classroom-toc-section"));
                for (int j = 0; j < sections.size(); j++) {
                    String chapterName = sections.get(j).findElement(By.cssSelector("button[class*='classroom-toc-section__toggle'] span")).getText().trim()
                            .replaceAll("[0-9.]", "").trim();
                    Thread.sleep(500);
                    Boolean clickableElement = sections.get(j).getAttribute("class").contains("classroom-toc-section--collapsed");
                    if (clickableElement) {
                        sections.get(j).click();
                    }
                    appendTextIntoDocx(chapterName, 16);

                    List<WebElement> videos = null;
                    videos = sections.get(j).findElements(By.cssSelector("ul.classroom-toc-section__items  >  li  div[class*='classroom-toc-item__content']"));
                    for (int i = 0; i < videos.size(); i++) {
                        Thread.sleep(2000);
                        String OnlyVideoTittle = sections.get(j).findElements(By.cssSelector("div.classroom-toc-item__title")).get(i)
                                .getText().trim().replace("(Viewed)", "").trim();
                        String videoTittle = "Chapter " + (j + 1) + " :: " + chapterName + "_" + OnlyVideoTittle + " " + (i + 1) + "th";

                        if (!videoTittle.contains("Quiz")) {
                            videos.get(i).click();
                            Thread.sleep(1000);
                            waitUntilVideoLoaded();
                            appendTextIntoDocx(OnlyVideoTittle, 13);
                            System.out.println("VideoT Tittle ::" + videoTittle);
                            String videoUrl = driver.findElement(By.cssSelector("video[id*=video]")).getAttribute("src").trim();


                            String txtFile = rootPth + "/" + courseTittle + "_VideosURL.txt";

                            File fileName = new File(txtFile);
                            if (!fileName.exists()) {
                                fileName.createNewFile();
                            }

                            // Write text on txt file.
                            String writeStr = videoUrl + " =>" + videoTittle + "\n";
                            FileWriter fw = new FileWriter(fileName, true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            bw.write(writeStr);
                            bw.close();

                        }
                        Thread.sleep(500);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                driver.quit();
//                downloadVideo();
            }

            driver.quit();
//            downloadVideo();
        }
    }


    public static void downloadVideo() throws IOException {
        // Read text from file.
        String fileName = "";
        File[] files = new File(rootPth).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().endsWith("_VideosURL.txt")) {
                    fileName = file.getName().trim();
                }
            }
        }

        FileReader fr = new FileReader(rootPth + "/" + fileName);
        BufferedReader br = new BufferedReader(fr);
        String st;
        while ((st = br.readLine()) != null) {
            String videoUrl = st.split(" =>")[0].trim();
            String videoTittle = st.split(" =>")[1].trim().replaceAll("[^a-zA-Z0-9 _]", "").trim();
            System.out.println("Video download start -- :: " + videoTittle);

            try (BufferedInputStream in = new BufferedInputStream(new URL(Linkedin_Learning_Courses_Downloader.getFinalLocation(videoUrl)).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(rootPth + "/" + videoTittle + ".MP4")) {
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                System.out.println("Video download Complete :: " + videoTittle);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String getFinalLocation(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newLocation = conn.getHeaderField("Location");
                return getFinalLocation(newLocation);
            }
        }
        return address;
    }

    public static void customWait() throws InterruptedException {
        int count = driver.findElements(By.cssSelector("body.boot-complete.boot-complete")).size();
        int i = 0;
        while (count == 0 && i <= 180) {
            Thread.sleep(1000);
            count = driver.findElements(By.cssSelector("body.boot-complete.boot-complete")).size();
            i = i + 1;
        }
    }

    public static void waitUntilVideoLoaded() throws InterruptedException {
        int count = driver.findElements(By.cssSelector("video[id*=video]")).size();
        while (count == 0) {
            Thread.sleep(1000);
            count = driver.findElements(By.cssSelector("video[id*=video]")).size();
        }
    }

    public static void createDocxFile() throws IOException {
        String docFileName = rootPth + "/" + courseTittle + ".docx";
        try (XWPFDocument doc = new XWPFDocument()) {

            XWPFParagraph p1 = doc.createParagraph();
            p1.setAlignment(ParagraphAlignment.CENTER);

            // Set Text to Bold and font size to 22 for first paragraph
            XWPFRun r1 = p1.createRun();
            r1.setBold(true);
            r1.setFontSize(18);
            r1.setText("   " + courseTittle);
            r1.addBreak();
            r1.addBreak();

            // save the docs
            try (FileOutputStream out = new FileOutputStream(docFileName)) {
                doc.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void appendTextIntoDocx(String txt, int fontSize) throws InvalidFormatException, IOException {
        String docFileName = rootPth + "/" + courseTittle + ".docx";
        try (XWPFDocument doc = new XWPFDocument(OPCPackage.open(docFileName))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            XWPFParagraph paragraph = paragraphs.get(paragraphs.size() - 1);
            paragraph.setAlignment(ParagraphAlignment.LEFT);

            XWPFRun runText = paragraph.createRun();
            runText.addBreak();
            if (Objects.equals(fontSize, 13)) {
                runText.setBold(true);
                runText.setItalic(true);
                runText.setFontSize(fontSize);
                runText.setText("   " + txt);
            } else if (fontSize > 13) {
                runText.setBold(true);
                runText.setFontSize(fontSize);
                runText.setText(txt);
            } else {
                runText.setFontSize(fontSize);
                runText.setText(txt);
            }


            try (FileOutputStream out = new FileOutputStream(docFileName, true)) {
                doc.write(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
