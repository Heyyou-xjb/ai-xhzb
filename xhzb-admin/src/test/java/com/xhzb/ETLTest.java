package com.xhzb;


import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@SpringBootTest
public class ETLTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void readTxt() throws FileNotFoundException {
        //读取text文件
        InputStreamResource inputStreamResource = new InputStreamResource(new FileInputStream("E:\\abc.txt"));

        //创建TextReader
        TextReader textReader = new TextReader(inputStreamResource);

        //追加自定义信息
        textReader.getCustomMetadata().put("filename", "abc.txt");
        textReader.getCustomMetadata().put("type", "txt");

        //输出内容
        List<Document> documentList = textReader.read();
//        documentList.forEach(System.out::println);
    }

    @Test
    public void testPDFByPage() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

//        System.out.println(pdfReader.read());
    }

    @Test
    public void testPDFByParagraph() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\护理员工工作手册.pdf"));

        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

//        System.out.println(pdfReader.read());
    }

    @Test
    public void testTestSplitter() throws FileNotFoundException {

        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());


        // 创建TextSplitter
        TextSplitter textSplitter = new TokenTextSplitter();
        System.out.println("分隔之前的文档数：" + pdfReader.read().size());
        List<Document> documents = textSplitter.apply(pdfReader.read());
        System.out.println("分隔之后的文档数：" + documents.size());
//        System.out.println(documents);

        //存储到向量数据库中，分批添加，不然会报错
        /*for (int i = 0; i < documents.size(); i += 10) {
            vectorStore.add(documents.subList(i, Math.min(i + 10, documents.size())));
        }*/

        vectorStore.add(documents);
    }

    @Test
    public void retrieval() {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest
                .builder()
                .query("护理服务宗旨与核心价值是什么")
                .similarityThreshold(0.6)//只有相似度分数 ≥0.5 的文档才会被保留返回
                .topK(1)
                .build());
        documents.forEach(System.out::println);
    }
}
