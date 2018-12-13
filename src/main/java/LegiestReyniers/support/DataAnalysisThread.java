package LegiestReyniers.support;

import LegiestReyniers.model.DelayDayRecord;
import LegiestReyniers.model.DelaySingleRecord;
import LegiestReyniers.model.Tracked_station;
import LegiestReyniers.repositories.DelayDayRecordRepository;
import LegiestReyniers.repositories.DelaySingleRecordRepository;
import LegiestReyniers.repositories.TrackedStationRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Date;

@Component
@Scope("prototype")
@SuppressWarnings("Duplicates")
public class DataAnalysisThread implements Runnable{

    @Resource
    private DelaySingleRecordRepository delaySingleRecordRepository;

    @Resource
    private DelayDayRecordRepository delayDayRecordRepository;

    @Resource
    private TrackedStationRepository trackedStationRepository;

    private int index_id;

    @Override
    public void run() {

        System.out.println("-----------------TESTING THREAD --------------------------------------------");

        index_id=0;

        Iterable<Tracked_station> stations = trackedStationRepository.findAll();

        Iterable<DelaySingleRecord> rawData = delaySingleRecordRepository.findAll();


        for(Tracked_station ts: stations){

            JsonObject main = new JsonObject();

            main.addProperty("stationURI", ts.getUri());

            JsonArray avg = new JsonArray();
            JsonArray ontime = new JsonArray();
            JsonArray over = new JsonArray();

            for(DelaySingleRecord dsr: rawData){
                if(ts.getUri().equals(dsr.getStationuri())){
                    ontime.add(dsr.getNontime());
                    over.add(dsr.getNover());

                    int avger = 0;

                    if(dsr.getNdelay() !=0){
                        avger = dsr.getTotaldelay()/dsr.getNdelay();
                    }

                    avg.add(avger);
                }
            }

            main.add("ontime", ontime);
            main.add("over", over);
            main.add("avg", avg);

            try {

                String path = System.getProperty("user.dir") + "/Json/" + ts.getUri().substring(32) + ".json";

                FileWriter file = new FileWriter(path);
                file.write(main.toString());
                file.close();

                System.out.println("Geprint");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        runR();

        String path = System.getProperty("user.dir") + "/JsonProc/";

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();


        for(File file: listOfFiles){

            BufferedReader bufferedReader = null;

            try {
                bufferedReader = new BufferedReader(new FileReader(file.getCanonicalFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Gson gson = new Gson();
            JsonArray json = gson.fromJson(bufferedReader, JsonArray.class);

            DelayDayRecord record = new DelayDayRecord();


            record.setTotal_delay(json.get(0).getAsInt());
            record.setAverage_delay(json.get(1).getAsInt());
            record.setMax_delay(json.get(2).getAsInt());
            record.setMin_delay(json.get(3).getAsInt());
            record.setN_over30min(json.get(4).getAsInt());
            record.setN_ontime(json.get(5).getAsInt());

            int i = (int) (new Date().getTime()/1000);

            record.setTimestamp(i);
            record.setStationuri(file.getName());
            record.setId(index_id);

            delayDayRecordRepository.save(record);

            System.out.println(json.getClass());
            System.out.println(json.toString());


            System.out.println(json.get(0));
            System.out.println(json.get(1));


            index_id++;


        }




        System.out.println("-----------------Ending THREAD------------------------------------------------");









        //Get all objects from the server
        //Delete al current items
        //Put all the items in R
        //Evaulate
        //



    }

    public void runR(){


        String s = null;

        try {

            //Process aanmaken waarin we
            ProcessBuilder builder = new ProcessBuilder("RScript", "verwerking.R");
            builder.redirectErrorStream(true);
            Process p = builder.start();

            //Reader die de output van het command zal uitlezen
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            StringBuilder sb = new StringBuilder();

            while (true) {
                line = r.readLine();

                if (line == null) {
                    break;
                }

                if (!line.contains("Columns"))
                    sb.append(line);
            }

            s = sb.toString();

            System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
