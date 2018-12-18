package freelance.tracking.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import freelance.tracking.dao.entity.AdInfo;
import freelance.tracking.dao.entity.AdStat;
import freelance.tracking.dao.entity.Param;
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class Utility {

    private static AdDAO adDAO;

    private static ObjectMapper mapper = new ObjectMapper();
    private static CloseableHttpClient httpclient = HttpClients.createDefault();
    private static Drive driveService;

    private static HttpTransport HTTP_TRANSPORT;
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            driveService = createDriveService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    public void setAdDAO(AdDAO adDAO) {
        Utility.adDAO = adDAO;
    }

    private static String jsonPost(String url, String reqBody) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(reqBody, Charset.forName("utf-8")));

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000).build();
        httpPost.setConfig(config);

        CloseableHttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200)
            throw new Exception("JsonPost не удалось выполнить запрос, код ошибки: " + response.getStatusLine().getStatusCode());

        String body = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());

        return body;
    }

    public static void requestUpdate(List<Schedule> notFull) {
        try {
            List<String> tokens = notFull.stream().map(nf -> "trk_" + nf.getId()).collect(Collectors.toList());
            String body = jsonPost("http://localhost:8080/historyByTokens", mapper.writeValueAsString(tokens));
            for (JsonNode task : mapper.readTree(body)) {
                try {
                    Integer taskID = Integer.parseInt(task.get("nick").asText().substring(4));
                    Optional<Schedule> target = notFull.stream().filter(nf -> nf.getId() == taskID).findFirst();
                    target.ifPresent(t -> t.updateInfo(Schedule.builder()
                            .id(t.getId())
                            .time(t.getTime())
                            .report(task.get("reportUrl").asText())
                            .status(Status.valueOf(task.get("status").asText())).build())
                    );
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkDownloaded(String taskID, List<Schedule> schedules) {

        String PATH = "reports/" + taskID;

        File directory = new File(PATH);
        if (!directory.exists())
            directory.mkdir();

        schedules.stream().filter(sh -> sh.getStatus() == Status.COMPLETE).forEach(
                schedule -> {
                    String name = PATH + "/" + schedule.getId() + ".xlsx";
                    if (!new File(name).exists())
                        downloadReport(name, schedule.getReport());
                }
        );
    }

    private static void downloadReport(String name, String url) {

        try {
            url = url.substring(0, url.lastIndexOf('/'));
            String fileId = url.substring(url.lastIndexOf('/') + 1);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            driveService.files().export(fileId, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .executeMediaAndDownloadTo(byteArrayOutputStream);

            try (OutputStream outputStream = new FileOutputStream(name)) {
                byteArrayOutputStream.writeTo(outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Drive createDriveService() throws IOException {
        String KEY_FILE = "olx-parser.json";
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(KEY_FILE), HTTP_TRANSPORT, JSON_FACTORY);
        credential = credential.createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        String APPLICATION_NAME = "OLX Parser";
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static List<AdInfo> prepareData(List<Schedule> schedules, String taskID) {

        String PATH = String.format("reports/%s/", taskID);

        List<AdInfo> adStats = new ArrayList<>();
        for (Schedule schedule : schedules) {
            int id = schedule.getId();

            try {
                XSSFWorkbook out = new XSSFWorkbook(new FileInputStream(PATH + id + ".xlsx"));
                XSSFSheet sheet = out.getSheetAt(0);

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    XSSFRow row = sheet.getRow(i);

                    try {
                        AdInfo ad = new AdInfo();

                        String url = getDataFromRow(row, 13);
                        ad.setId(url.substring(url.lastIndexOf("_") + 1));
                        ad.setUrl(new Param(url));

                        ad.setTitle(new Param(getDataFromRow(row, 1)));
                        ad.setPosition(new Param(getDataFromRow(row, 0)));
                        ad.setPrice(new Param(getDataFromRow(row, 2)));
                        ad.setStats(new Param(String.format("%s (+%s)",
                                getDataFromRow(row, 4), getDataFromRow(row, 5))));

                        String prom = getDataFromRow(row, 7);
                        ad.setPremium(new Param(prom.contains("1") ? "1" : "0"));
                        ad.setVip(new Param(prom.contains("2") ? "1" : "0"));
                        ad.setUrgent(new Param(prom.contains("3") ? "1" : "0"));
                        ad.setUpped(new Param(prom.contains("4") ? "1" : "0"));

                        adStats.add(ad);
                    } catch (Exception ignore) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return adStats;
    }

    private static String getDataFromRow(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        try {
            switch (cell.getCellType()) {
                case STRING: return cell.getStringCellValue();
                case NUMERIC: return String.valueOf(cell.getNumericCellValue()).replace(".0", "");
                default: return "";
            }
        } catch (Exception ignore) {
            return "";
        }
    }

    public static HashMap<String, String> parseTaskParams(String jsonParams) throws IOException {
        JsonNode jsonNode = mapper.readTree(jsonParams);

        System.out.println("---------------------------------------------------");
        System.out.println("Параметры запроса:");
        HashMap<String, String> result = new HashMap<>();
        for (JsonNode param : jsonNode.get("parameters")) {
            String name = param.get("name").asText();
            String value = param.get("value").asText();

            if (!value.isEmpty()) {
                result.put(name, value);
                System.out.println("    *" + name + ": " + value);
            }
        }

        return result;
    }
}
