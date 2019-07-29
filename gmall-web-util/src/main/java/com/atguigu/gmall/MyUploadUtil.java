package com.atguigu.gmall;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MyUploadUtil {


    public static  String uplodImage(MultipartFile file){
    String url="http://192.168.70.111";


        String path=MyUploadUtil.class.getClassLoader().getResource("traker.properties").getPath();
        try {
            ClientGlobal.init(path);
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer connection = trackerClient.getConnection();
            StorageClient storageClient = new StorageClient(connection,null);

            String filename = file.getOriginalFilename();
            int i = filename.lastIndexOf(".");
            filename= filename.substring(i+1);
            String[] jpgs = storageClient.upload_file(file.getBytes(), filename, null);
            for (String jpg : jpgs) {
                url+="/"+jpg;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        return url;
    }
}
