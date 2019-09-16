package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {

    private static final List<String> CONTENT_TYPES = Arrays.asList("image/gif","image/jpeg");
    private static Logger logger = LoggerFactory.getLogger(UploadService.class);

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        try {
            //校验文件类型
            String contentType = file.getContentType();
            if(!CONTENT_TYPES.contains(contentType)){
                logger.info("文件类型不合法：{]", originalFilename);
                return null;
            }

            //校验文件内容
            BufferedImage read = ImageIO.read(file.getInputStream());
            if(read == null){
                logger.info("文件内容不合法：{]", originalFilename);
                return null;
            }
            //保存到服务器
            //file.transferTo(new File("E:\\JetBrains\\IdeaProjects\\leyou-image\\" + originalFilename));
            //fastclient方式
            String ext = StringUtils.substringAfterLast(originalFilename, ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);
            return "http://image.leyou.com/" + storePath.getFullPath();
            //返回url,进行回显
            //return "http://image.leyou.com/" + originalFilename;
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("文件保存异常：{]", originalFilename);

        }
        return null;
    }
}
