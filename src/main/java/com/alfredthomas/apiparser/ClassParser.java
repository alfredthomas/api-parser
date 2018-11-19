package com.alfredthomas.apiparser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ClassParser {

    static void parseAllClasses(Document allClassDocument,String outputName)
    {
        iterateThroughAllClasses(allClassDocument,outputName,false);
    }
    static void downloadAllClassesHTML(Document allClassDocument,String outputName)
    {
        iterateThroughAllClasses(allClassDocument,outputName,true);
    }
    private static void iterateThroughAllClasses(Document allClassDocument,String outputName, boolean asHTML)
    {
        if(allClassDocument==null)
            return;
        try {
            String baseURL = allClassDocument.location().substring(0, allClassDocument.location().lastIndexOf('/'));
            Elements list = allClassDocument.getElementsByTag("li");

            System.out.println("***Starting Zip of "+outputName+".zip***");
            FileOutputStream fileOutputStream = new FileOutputStream(outputName + ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);


            JSONArray jsonClassList = new JSONArray();
            for (Element aList : list) {
                Element element = aList.getElementsByTag("a").first();
                //parsing code goes here

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", element.text());

                String path = element.attr("abs:href");

                //remove the .html
                String outPath = path.substring(0, path.lastIndexOf('.'));
                jsonObject.put("path", outPath);
                jsonClassList.put(jsonObject);

                //stringToWrite is either parsed JSON or HTML file accordingly
                String stringToWrite;
                Document nextDocument = Util.readRemoteHTMLFile(path);
                if(!asHTML) {
                    System.out.println("PARSING: " + outPath);
                    stringToWrite = parseHTMLFile(nextDocument).toString();
                }
                else
                {
                    stringToWrite = getHTMLFile(nextDocument);
                }
                if(stringToWrite == null || stringToWrite.isEmpty())
                    continue;
                ZipEntry zipEntry=new ZipEntry(outPath.substring(baseURL.length() + 1) + (asHTML?".html":".json"));
                zipOutputStream.putNextEntry(zipEntry);
                System.out.println("WRITING: " + outPath + " to " + outputName + ".zip");
                zipOutputStream.write(stringToWrite.getBytes());
                zipOutputStream.closeEntry();
//                Util.writeToFile(ClassParser.parseHTMLFile(Util.readRemoteHTMLFile(path)), outputName + outPath.substring(baseURL.length()));

            }


            JSONObject jsonObject = new JSONObject();
            jsonObject.put("classes", jsonClassList);
            zipOutputStream.putNextEntry(new ZipEntry("class_list"+(asHTML?".html":".json")));
            if(!asHTML) {
                zipOutputStream.write(jsonObject.toString().getBytes());
            }
            else
            {
                zipOutputStream.write(allClassDocument.outerHtml().getBytes());

            }
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            fileOutputStream.close();
//            Util.writeToFile(jsonObject, outputName, "class_list");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private static String getHTMLFile(Document document)
    {
        if(document == null)
            return null;
        return document.outerHtml();
    }
    private static JSONObject parseHTMLFile(Document document)
    {

        JSONObject classObject = new JSONObject();

        if(document ==null)
            return classObject;

        Elements contentContainerCheck = document.getElementsByClass("contentContainer");

        if(contentContainerCheck.isEmpty())
            return classObject;

        Element contentContainer = contentContainerCheck.first();

//        header
        parseHeader(document.getElementsByClass("header"),classObject);

//        inheritance
        parseInheritance(contentContainer.getElementsByClass("inheritance"),classObject);

//        description
        parseDescription(contentContainer.getElementsByClass("description"),classObject);

//        summary
        parseSummary(contentContainer.getElementsByClass("summary"),classObject);

//        details
        parseDetails(contentContainer.getElementsByClass("details"),classObject);
        return classObject;
    }
    private static void parseHeader(Elements headerElement, JSONObject classObject)
    {
        if(!headerElement.isEmpty())
        {
            JSONObject header = new JSONObject();
            JSONArray subtitles = new JSONArray();
            for (Element subtitle : headerElement.first().getElementsByClass("subTitle")) {
                addIfNotEmpty(subtitles,subtitle.text());
            }
            addIfNotEmpty(header,subtitles,"subtitles");
            addIfNotEmpty(header,headerElement.first().getElementsByClass("title").text(),"title");
            addIfNotEmpty(classObject,header,"header");
        }
    }
    private static void parseInheritance(Elements inheritanceElements, JSONObject classObject)
    {
        if(!inheritanceElements.isEmpty()) {
            JSONArray inheritanceArray = new JSONArray();
            for (Element element : inheritanceElements) {
                Element link = element.child(0);
                if (!link.children().isEmpty())
                    link = link.child(0);
                addIfNotEmpty(inheritanceArray,parseLink(link));
            }

            addIfNotEmpty(classObject,inheritanceArray,"inheritance");
        }
    }
    private static void parseDescription(Elements descriptionElement, JSONObject classObject)
    {
        JSONObject descriptionObject = new JSONObject();
        if(!descriptionElement.isEmpty())
        {
            Element descriptionContainer = descriptionElement.first().getElementsByTag("li").first();
            boolean finishedHeaders = false;
            JSONArray headerDefinitions = new JSONArray();
            JSONArray footerDefinitions = new JSONArray();
            for(Element child: descriptionContainer.children())
            {
                if(child.tagName().equals("hr"))
                {
                    child.remove();
                    finishedHeaders= true;
                }

                else if(child.tagName().equals("dl"))
                {
                    if(!finishedHeaders)
                        addIfNotEmpty(headerDefinitions, parseDefinition(child));
                    else
                        addIfNotEmpty(footerDefinitions, parseDefinition(child));
                    child.remove();
                }
            }
            addIfNotEmpty(descriptionObject,headerDefinitions,"header_definitions");
            addIfNotEmpty(descriptionObject,footerDefinitions,"footer_definitions");
            addIfNotEmpty(descriptionObject,parseBlock(descriptionContainer),"body");
        }
        addIfNotEmpty(classObject,descriptionObject,"description");
    }
    private static void parseSummary(Elements summaryElement, JSONObject classObject)
    {
        JSONArray summaries = new JSONArray();
        if(!summaryElement.isEmpty())
        {
            Elements summaryListElements =  summaryElement.first().select(":root > ul > li > ul > li");

            for(Element summaryObject:summaryListElements) {
                JSONObject summaryJSONObject = new JSONObject();

                addIfNotEmpty(summaryJSONObject,getHeaderText(summaryObject),"title");
                Element tableElement = summaryObject.getElementsByTag("table").first();
                addIfNotEmpty(summaryJSONObject,parseTable(tableElement),"summary_table");

                JSONArray summaryAdditionalArray = new JSONArray();

                Elements summaryObjectAdditional = summaryObject.select(":root> ul > li");
                for(Element element: summaryObjectAdditional)
                {
                    JSONObject summaryAdditional = new JSONObject();
                    addIfNotEmpty(summaryAdditional,getHeaderText(element),"title");
                    addIfNotEmpty(summaryAdditional,parseBlock(element.getElementsByTag("code").first()),"methods");
                    addIfNotEmpty(summaryAdditionalArray,summaryAdditional);
                }
                addIfNotEmpty(summaryJSONObject,summaryAdditionalArray,"summary_additional");
                addIfNotEmpty(summaries,summaryJSONObject);
            }

        }
        addIfNotEmpty(classObject,summaries,"summary");
    }
    private static void parseDetails(Elements detailsElement, JSONObject classObject)
    {
        JSONArray detailsArray = new JSONArray();
        if(!detailsElement.isEmpty())
        {
            for(Element detail: detailsElement.select(":root > ul > li > ul"))
            {
                JSONObject detailObject = new JSONObject();
                addIfNotEmpty(detailObject,detail.select("h1,h2,h3,h4,h5,h6").first().text(),"name");
                JSONArray methods = new JSONArray();
                for(Element method: detail.select(":root > li > ul"))
                {
                    JSONObject methodObject = parseMethod(method);
                    if(!methodObject.isEmpty())
                        addIfNotEmpty(methods,methodObject);
                }
                if(!methods.isEmpty())
                    addIfNotEmpty(detailObject,methods,"methods");

                addIfNotEmpty(detailsArray,detailObject);
            }
        }
        addIfNotEmpty(classObject,detailsArray,"details");

    }
    private static JSONObject parseTable(Element tableElement)
    {
        if(tableElement == null)
            return null;
        JSONObject tableJSONObject = new JSONObject();
        Elements tableRows = tableElement.getElementsByTag("tr");
        JSONArray headers = new JSONArray();
        JSONArray rows = new JSONArray();

        Elements tableHeaderElements = tableRows.get(0).getElementsByTag("th");
        for (Element tableHeaderElement : tableHeaderElements) {
            addIfNotEmpty(headers, tableHeaderElement.text());
        }
        addIfNotEmpty(tableJSONObject,headers,"headers");

        for(int i = 1; i<tableRows.size();i++)
        {
            Elements rowElements = tableRows.get(i).getElementsByTag("td");
            JSONArray rowArray = new JSONArray();
            for (Element cell:rowElements) {
                addIfNotEmpty(rowArray,parseBlock(cell));
            }
            addIfNotEmpty(rows,rowArray);
        }
        addIfNotEmpty(tableJSONObject,rows,"rows");

        return tableJSONObject;
    }
    private static JSONObject parseList(Element listElement)
    {
        JSONObject tableJSONObject = new JSONObject();
        Elements listRows = listElement.children();
        JSONArray rows = new JSONArray();

        for (Element listRow : listRows) {
            addIfNotEmpty(rows, parseBlock(listRow));
        }
        addIfNotEmpty(tableJSONObject,rows,"rows");
        return tableJSONObject;
    }
    private static JSONObject parseBlock(Element element)
    {
        JSONObject block = new JSONObject();
        if(element==null)
            return block;
        //replace lists
        int listCount = 0;
        for(Element list: element.getElementsByTag("ul"))
        {
            if(list == null || list.parent()==null)
                continue;
            JSONObject jsonObject = parseList(list);
            if (jsonObject.isEmpty())
                continue;
            listCount++;

            addIfNotEmpty(block,jsonObject,"l"+listCount);

            replaceElement(list,"l",listCount);

        }

        //replace tables
        int tableCount = 0;
        for(Element table: element.getElementsByTag("table"))
        {
            //already replaced(for nested tables)
            if(table == null || table.parent()==null)
                continue;
            tableCount++;
            addIfNotEmpty(block,parseTable(table),"t"+tableCount);
            replaceElement(table,"t",tableCount);
        }

        //replace links
        for(Element link: element.getElementsByTag("a"))
        {
            replaceLinkInline(link);
        }


        addIfNotEmpty(block,element.html().replaceAll("(</?code>)+",""),"text");
        return block;
    }
    private static JSONArray parseDefinition(Element element)
    {
        JSONArray definitionArray = new JSONArray();

        if(!element.tagName().equals("dl"))
        {
            Elements listElements = element.getElementsByTag("dl");
            element = listElements.isEmpty()? null:listElements.first();
        }
        if(element == null || element.children().isEmpty())
            return definitionArray;

        JSONObject definitionObject = null;
        JSONArray definitionObjectDefinitions = null;
        for(int i = 0; i<element.children().size();i++)
        {
            Element child = element.child(i);
            if(child.tagName().equals("dt"))
            {
                addIfNotEmpty(definitionObject,definitionObjectDefinitions,"description");
                addIfNotEmpty(definitionArray,definitionObject);

                definitionObject = new JSONObject();
                definitionObjectDefinitions = new JSONArray();
                addIfNotEmpty(definitionObject,child.text(),"term");
            }
            else if (child.tagName().equals("dd"))
            {
                addIfNotEmpty(definitionObjectDefinitions,parseBlock(child));
            }
        }
        //catch last case end of loop
        addIfNotEmpty(definitionObject,definitionObjectDefinitions,"description");


        addIfNotEmpty(definitionArray,definitionObject);



        return definitionArray;
    }
    private static JSONObject parseLink(Element element)
    {
        JSONObject link = new JSONObject();
        if(!element.tagName().equals("a") && !element.getElementsByTag("a").isEmpty())
            element = element.selectFirst("a");
        addIfNotEmpty(link,element.text(),"text");
        if(element.hasAttr("href"))
        {
            String url = element.attr("href");
            if(url.contains("#"))
            {
//                System.out.println(url);
                addIfNotEmpty(link,url.substring(url.indexOf('#')+1),"section");
                url = url.substring(0,url.indexOf('#'));
            }
            addIfNotEmpty(link,Util.sanitizeURL(url),"path");


        }
        return link;
    }
    private static JSONObject  getHeaderText(Element root)
    {
        //if some api have different hsize
        Element headerElement =  root.select("h1,h2,h3,h4,h5,h6").first();
        return parseBlock(headerElement);
    }
    private static JSONObject parseMethod(Element method)
    {
        JSONObject methodObject = new JSONObject();

        addIfNotEmpty(methodObject,getHeaderText(method),"title");

        Elements declaration = method.getElementsByTag("pre");
        addIfNotEmpty(methodObject,parseBlock(declaration.first()),"declaration");

        Elements depricated = method.getElementsByClass("deprecationComment");
        addIfNotEmpty(methodObject,parseBlock(depricated.first()),"depricated");


        Elements description = method.getElementsByTag("div");
        addIfNotEmpty(methodObject,parseBlock(description.last()),"description");

        Elements definitions = method.getElementsByTag("dl");
        if(!definitions.isEmpty()) {
            addIfNotEmpty(methodObject,parseDefinition(definitions.first()),"definitions");
        }

        return methodObject;
    }
    private static void replaceElement(Element element, String tag, int count)
    {
        TextNode replacement = new TextNode("$$"+tag+count+"$$");
        element.replaceWith(replacement);
    }
    private static void addIfNotEmpty(Object addTo, Object newObject, String key)
    {
        boolean add = false;
        if(newObject instanceof JSONObject)
        {
            if(!((JSONObject) newObject).isEmpty())
                add = true;
        }
        else if (newObject instanceof JSONArray)
        {
            if(!((JSONArray) newObject).isEmpty())
                add = true;        }
        else if (newObject instanceof String)
        {
            if(!((String) newObject).isEmpty())
                add = true;
        }

        if(add)
        {
            if(addTo instanceof JSONObject)
                ((JSONObject)addTo).put(key,newObject);

            else if(addTo instanceof JSONArray)
                ((JSONArray) addTo).put(newObject);
        }
    }
    private static void addIfNotEmpty(JSONArray addTo, Object newObject)
    {
        addIfNotEmpty(addTo, newObject,null);
    }
    private static void replaceLinkInline(Element link)
    {
        String url = link.attr("href");
        if(!url.isEmpty())
        {
            int index = url.indexOf('#');
            StringBuilder urlBuilder = new StringBuilder();

            if(index>0) {
                urlBuilder.append(Util.sanitizeURL(url.substring(0, index)));
                urlBuilder.append('#');
            }
            urlBuilder.append(Util.sanitizeURL(url.substring(index+1)));
            for (Attribute attribute:link.attributes()) {
                if(attribute.getKey().equals("href"))
                    attribute.setValue(urlBuilder.toString());
                else
                    link.removeAttr(attribute.getKey());
            }
            if(link.children().size()>0)
            {
                TextNode childToString = new TextNode(link.text());
                for(Element child: link.children())
                {
                    child.remove();
                }
                link.appendChild(childToString);
            }
        }
    }
    private static Document convertLinksToLocal(Document document)
    {
        if(document == null)
            return null;
        for (Element element:document.getElementsByTag("a")) {
            if(element.attributes().hasKeyIgnoreCase("href"))
                element.baseUri();
                //element.setBaseUri();

        }
        return document;
    }
}
