package com.alfredthomas.apiparser;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {
    public static final String baseInPath = "in/";
    public static final String baseOutPath = "out/";


    public static void writeToFile(JSONObject jsonObject,String path, String filename)
    {
        if(jsonObject == null || jsonObject.isEmpty())
            return;
        writeFile(jsonObject, createFilePath(path,filename,false));
    }
    public static void writeToFile(JSONObject jsonObject,String path)
    {
        if(jsonObject == null || jsonObject.isEmpty() || path == null || path.isEmpty())
            return;
        writeFile(jsonObject,createFilePath(path,false));
    }
    private static void writeFile(JSONObject jsonObject,File file)
    {
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jsonObject.toString());
            fileWriter.flush();
            fileWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public static Document readRemoteHTMLFile(String path)
    {
        try{
            return Jsoup.connect(path).get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    public static Document readLocalHTMLFile(String path, String filename)
    {
        return  readFile(createFilePath(path,filename,true));
    }
    public static Document readLocalHTMLFile(String path)
    {
        return readFile(createFilePath(path,true));
    }
    public static Document readFile(File file) {
        try {
            return Jsoup.parse(file, "UTF-8");
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private static File createFilePath(String path,boolean input)
    {
        if(path.indexOf('/')<0)
            return createFilePath(null,path,input);

        return createFilePath(path.substring(0,path.lastIndexOf('/')),path.substring(path.lastIndexOf('/')+1),input);
    }
    private static File createFilePath(String path, String filename, boolean input)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if(input)
            stringBuilder.append(baseInPath);
        else
            stringBuilder.append(baseOutPath);

        if(path != null)
            stringBuilder.append(path);

        if(stringBuilder.charAt(stringBuilder.length()-1)!= '/')
            stringBuilder.append('/');

        File directory = new File(stringBuilder.toString());
        if(!directory.exists() && !input)
        {
            directory.mkdirs();
        }

        //add filename and suffix
        stringBuilder.append(filename);
        if(input)
            stringBuilder.append(".html");
        else
            stringBuilder.append(".json");

        return new File(stringBuilder.toString());
    }
    public static Document getAllClassDocument(String currentPath) throws FileNotFoundException
    {
        Document currentPage = Util.readRemoteHTMLFile(currentPath);

        try {
            Element allClassNavButton = currentPage.getElementById("allclasses_navbar_top").getElementsByTag("a").first();
            String url = allClassNavButton.attr("abs:href");
            return Util.readRemoteHTMLFile(url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new FileNotFoundException("Cannot find allclasses file");
        }
    }
    public static String sanitizeURL(String current)
    {
//        StringBuilder stringBuilder = new StringBuilder(current);
//        while( stringBuilder.substring(0,2).equals("../"))
//        {
//            stringBuilder.delete(0,2);
//        }
        if(current.isEmpty())
            return current;
        int startIndex = 0;
        while(startIndex< current.length()-3 && current.substring(startIndex,startIndex+3).equals("../"))
        {
            startIndex+=3;
        }

        String sanitized = current.substring(startIndex);
        if(sanitized.endsWith(".html"))
            sanitized = sanitized.substring(0,sanitized.lastIndexOf('.'));

        return sanitized;
    }
    public static void zipOutput(String path)
    {
        try{
            System.out.println("***Starting Zip of "+path+".zip***");

            List<File> fileList = getAllFiles(new File(baseOutPath+path));

            FileOutputStream fileOutputStream = new FileOutputStream(baseOutPath+path+".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            for(File file:fileList)
            {
                FileInputStream fileInputStream = new FileInputStream(file);
                System.out.println("WRITING: "+file.getName()+" to "+path+".zip");
                ZipEntry zipEntry = new ZipEntry(file.getPath().substring(file.getPath().indexOf('\\')+1));
                zipOutputStream.putNextEntry(zipEntry);

                final byte[] bytes = new byte[1024];
                int length;
                while ((length = fileInputStream.read(bytes)) >= 0) {
                    zipOutputStream.write(bytes, 0, length);
                }

                zipOutputStream.closeEntry();
                fileInputStream.close();
            }

            zipOutputStream.close();
            fileOutputStream.close();
            System.out.println("***Zipping Complete!***");

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    private static List<File> getAllFiles(File dir)
    {
        List<File> fileList = new ArrayList<>();
        try{
            for(File file: dir.listFiles())
            {
                if(file.isDirectory())
                {
                    fileList.addAll(getAllFiles(file));
                }
                else
                    fileList.add(file);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return fileList;
    }
}
