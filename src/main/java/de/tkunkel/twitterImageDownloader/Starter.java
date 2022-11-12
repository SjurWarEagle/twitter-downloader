package de.tkunkel.twitterImageDownloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tkunkel.twitterImageDownloader.types.config.Configuration;
import de.tkunkel.twitterImageDownloader.types.metainfo.MetaInfo;
import de.tkunkel.twitterImageDownloader.types.metainfo.MetaInfoImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackageClasses = Starter.class)
public class Starter {
    @Autowired
    Configuration configuration;

    public static void main(String[] args) {
        SpringApplication.run(Starter.class, args);
    }

    //at start and once per hour
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void regularly() throws InterruptedException, IOException {
        start();
    }

    //    @PostConstruct
    private void start() throws InterruptedException, IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(configuration.twitterOAuthConsumerKey);
        cb.setOAuthConsumerSecret(configuration.twitterOAuthConsumerSecret);
        cb.setOAuthAccessToken(configuration.twitterOAuthAccessToken);
        cb.setOAuthAccessTokenSecret(configuration.twitterOAuthAccessTokenSecret);
        cb.setGZIPEnabled(true);
        cb.setTweetModeExtended(true);

        Twitter twitter = new TwitterFactory(cb.build()).getInstance();

        int pageno = 1;
        String user = "Phelorena";
        List<twitter4j.Status> statuses = new ArrayList<>();

        while (true) {

            try {

                int size = statuses.size();
                Paging page = new Paging(pageno++, 100);
                statuses.addAll(twitter.getUserTimeline(user, page));
                if (statuses.size() == size)
                    break;
            } catch (TwitterException e) {

                e.printStackTrace();
            }
        }

        this.downloadImagesFromTweets(statuses);
    }

    private void downloadImagesFromTweets(List<Status> statuses) throws InterruptedException, IOException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        MetaInfo metaInfo = new MetaInfo();

        System.out.println("Total: " + statuses.size());
        for (Status status : statuses) {
            if (status.isRetweet()) {
                continue;
            }
            if (status.getInReplyToStatusId() != -1) {
                continue;
            }
            System.out.println(status.getText());

            for (MediaEntity mediaEntity : status.getMediaEntities()) {
//                executor.submit(() -> {
                downloadMedia(mediaEntity.getMediaURL(), mediaEntity.getId() + "");
                rememberMetaData(metaInfo, status, mediaEntity.getId());
//                });
            }
        }

        executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(1, TimeUnit.HOURS);
        BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(configuration.imageMetadataFile));
        bufferedWriter.write(gson.toJson(metaInfo));

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    private void rememberMetaData(MetaInfo metaInfo, Status status, long mediaId) {
        MetaInfoImage newInfo = new MetaInfoImage();
        metaInfo.info.add(newInfo);

        String postingText = status.getText().replaceAll("((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?+-=\\\\.&]*)", "").trim();

        newInfo.mediaId = mediaId + "";
        newInfo.twitterId = status.getId() + "";
        newInfo.twitterPost = postingText;
        newInfo.twitterPostTimestamp = status.getCreatedAt().getTime();
    }

    public static void downloadMedia(String mediaURL, String imageName) {
        System.out.println("💾 Downloading " + mediaURL + " with " + imageName);
        try {
            URL url = new URL(mediaURL);
            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
            byte[] response = out.toByteArray();
            String fileName = "./images/" + imageName + ".jpg";
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(response);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
