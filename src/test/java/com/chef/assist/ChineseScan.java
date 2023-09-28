package com.chef.assist;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseScan {

    public static void main(String[] args) {
        String folderPath = "src/main/java/com/chef/assist/controller"; // 指定文件夹路径
        scanFolder(new File(folderPath));
    }

    public static void scanFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        scanFile(file);
                    } else if (file.isDirectory()) {
                        scanFolder(file);
                    }
                }
            }
        }
    }

    public static void scanFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                findChineseStrings(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void findChineseStrings(String line) {
        Pattern pattern =  Pattern.compile("\"([\\u4e00-\\u9fa5]+(?:，[\\u4e00-\\u9fa5]+)*)"); // 匹配中文字符的正则表达式
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String chineseString = matcher.group(1);
            System.out.println(chineseString);
        }
    }
}
