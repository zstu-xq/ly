package com.leyou.goods.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;

@Service
public class GoodsHtmlService {

    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private GoodsService goodsService;

    public void createHtml(Long id){

        //初始化运行上下文
        Context context = new Context();
        //设置数据模型
        context.setVariables(this.goodsService.loadData(id));
        PrintWriter printWriter = null;
        try {
            File file = new File("E:\\java\\nginx-1.16.0\\html\\item\\" + id + ".html");
            printWriter = new PrintWriter(file);

            templateEngine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }finally {
            if(printWriter != null){
                printWriter.close();
            }
        }

    }

    public void deleteeHtml(Long id) {
        File file = new File("E:\\java\\nginx-1.16.0\\html\\item\\" + id + ".html");
        file.deleteOnExit();
    }
}
