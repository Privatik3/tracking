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
import freelance.tracking.dao.entity.schedule.Schedule;
import freelance.tracking.dao.entity.schedule.Status;
import freelance.tracking.dao.entity.task.Param;
import freelance.tracking.dao.entity.task.Parameters;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utility {

    private static final String TASK_MANAGER_IP = "localhost";
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

    public static void clearTaskReports(Integer taskID) {

        try {
            File taskFolder = new File("reports/" + taskID);
            if (taskFolder.exists()) {
                String[] entries = taskFolder.list();
                for (String s : entries) {
                    File currentFile = new File(taskFolder.getPath(), s);
                    currentFile.delete();
                }
                taskFolder.delete();
            }
        } catch (Exception e) {
            System.err.println("Не удалось зачистить папку таска");
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
            List<String> tokens = notFull.stream().map(nf -> String.format("trk_%s_%s", nf.getTaskId(), nf.getTime())).collect(Collectors.toList());
            String body = jsonPost("http://" + TASK_MANAGER_IP + ":8080/historyByTokens", mapper.writeValueAsString(tokens));
            for (JsonNode task : mapper.readTree(body)) {
                try {
                    String[] nick = task.get("nick").asText().split("_");
                    Integer taskId = Integer.parseInt(nick[1]);
                    Integer time = Integer.parseInt(nick[2]);
                    Optional<Schedule> target = notFull.stream().filter(
                            nf -> (nf.getTaskId() == taskId && nf.getTime() == time)).findFirst();
                    target.ifPresent(t -> t.updateInfo(Schedule.builder()
                            .id(t.getId())
                            .taskId(t.getTaskId())
                            .time(t.getTime())
                            .report(task.get("reportUrl").asText())
                            .status(Status.valueOf(task.get("status").asText())).build())
                    );
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkDownloaded(int taskID, List<Schedule> schedules) {

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

    public static List<AdInfo> prepareData(List<Schedule> schedules, int taskID) {

        String PATH = String.format("reports/%s/", taskID);

        List<AdInfo> adStats = new ArrayList<>();
        for (Schedule schedule : schedules) {
            int id = schedule.getId();

            try {
                XSSFWorkbook out = new XSSFWorkbook(new FileInputStream(PATH + id + ".xlsx"));
                XSSFSheet sheet = out.getSheetAt(0);

                int urlOffset = 0, titleOffset = 0, allViewOffset = 0, todayViewOffset = 0;
                int positionOffset = 0, priceOffset = 0, promOffset = 0, upTimeOffset = 0;

                XSSFRow header = sheet.getRow(0);
                for (int i = 0; i <= header.getLastCellNum(); i++) {
                    switch (getDataFromRow(header, i)) {
                        case "Позиция": positionOffset = i; break;
                        case "Цена": priceOffset = i; break;
                        case "Просм. Всего": allViewOffset = i; break;
                        case "Просм. Сегодня": todayViewOffset = i; break;
                        case "Методы продвижения": promOffset = i; break;
                        case "Ссылка": urlOffset = i; break;
                        case "Заголовок": titleOffset = i; break;
                        case "Время поднятия": upTimeOffset = i; break;
                    }
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    XSSFRow row = sheet.getRow(i);

                    try {
                        AdInfo ad = new AdInfo();

                        ad.setScheduleID(id);
                        ad.setUpTime(new SimpleDateFormat("yyyy.MM.dd HH:mm").parse(getDataFromRow(row, upTimeOffset)));

                        String url = getDataFromRow(row, urlOffset);
                        ad.setId(url.substring(url.lastIndexOf("_") + 1).replaceAll("\\?.*", ""));
                        ad.setUrl(new freelance.tracking.dao.entity.Param(url));

                        ad.setTitle(new freelance.tracking.dao.entity.Param(getDataFromRow(row, titleOffset)));
                        ad.setPosition(new freelance.tracking.dao.entity.Param(getDataFromRow(row, positionOffset)));
                        ad.setPrice(new freelance.tracking.dao.entity.Param(getDataFromRow(row, priceOffset)));
                        ad.setStats(new freelance.tracking.dao.entity.Param(String.format("%s (+%s)",
                                getDataFromRow(row, allViewOffset), getDataFromRow(row, todayViewOffset))));

                        String prom = getDataFromRow(row, promOffset);
                        ad.setPremium(new freelance.tracking.dao.entity.Param(prom.contains("1") ? "1" : "0"));
                        ad.setVip(new freelance.tracking.dao.entity.Param(prom.contains("2") ? "1" : "0"));
                        ad.setUrgent(new freelance.tracking.dao.entity.Param(prom.contains("3") ? "1" : "0"));
                        ad.setUpped(new freelance.tracking.dao.entity.Param(prom.contains("4") ? "1" : "0"));
                        ad.setXl(new freelance.tracking.dao.entity.Param(prom.contains("5") ? "1" : "0"));

                        adStats.add(ad);
                    } catch (Exception ignore) {
                    }
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
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    return String.valueOf(cell.getNumericCellValue()).replace(".0", "");
                default:
                    return "";
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

    public static void sendDelayTask(HashMap<String, String> param) throws Exception {
        jsonPost(
                "http://" + TASK_MANAGER_IP + ":8080/add_task",
                mapper.writeValueAsString(new Parameters(convertToPropFormat(param)))
        );
    }

    private static Param[] convertToPropFormat(HashMap<String, String> params) {
        Param[] result = new Param[params.size()];

        int paramIndex = 0;
        for (Map.Entry<String, String> param : params.entrySet())
            result[paramIndex++] = new Param(param.getKey(), param.getValue());

        return result;
    }

    public static boolean isNew(AdInfo ad) {
        int[] stats = parseStats(ad.getStats().toString());
        return stats[0] == stats[1];
    }

    public static int[] parseStats(String stats) {
        int[] result = new int[2];
        String[] stat = stats.split(" \\(\\+");
        result[0] = Integer.parseInt(stat[0]);
        result[1] = Integer.parseInt(stat[1].replace(")", ""));

        return result;
    }
}
