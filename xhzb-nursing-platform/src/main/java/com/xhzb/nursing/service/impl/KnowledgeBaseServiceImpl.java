package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.KnowledgeBaseMapper;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.xhzb.nursing.service.IKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.ai.document.Document;

import java.util.Arrays;

/**
 * 知识库Service业务层处理
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService {
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 查询知识库
     *
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KnowledgeBase selectKnowledgeBaseById(Long id) {
        return getById(id);
    }

    /**
     * 查询知识库列表
     *
     * @param knowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase) {
        return knowledgeBaseMapper.selectKnowledgeBaseList(knowledgeBase);
    }

    /**
     * 新增知识库
     *
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase) {
        //下载文件
        InputStream inputStream = ossAliyunFileStorageService.download(knowledgeBase.getDocumentUrl());
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        //读取文件
        PagePdfDocumentReader reader = new PagePdfDocumentReader(inputStreamResource, PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfTopTextLinesToDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build());

        //分割文件
        List<Document> documentList = reader.read();
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(500)  //目标块大小  token数
                .withMinChunkSizeChars(200) // 最小块的字符数
                .withMinChunkLengthToEmbed(10) // 最小的文本字符长度
                .withMaxNumChunks(10000)  //文档最大块数
                .withKeepSeparator(false)   //不保留换行符
                .build();
        List<Document> documents = tokenTextSplitter.apply(documentList);


        //存储到向量数据库中，分批添加，不然会报错
        for (int i = 0; i < documents.size(); i += 10) {
            vectorStore.add(documents.subList(i, Math.min(i + 10, documents.size())));
        }

        //收集所有文档ID
        List<String> docIdList = documents.stream().map(Document::getId).toList();
        String docIdListJSON = JSONUtil.toJsonStr(docIdList);

        //保存数据库
        knowledgeBase.setCreateTime(DateUtils.getNowDate());
        knowledgeBase.setRemark(docIdListJSON);
        boolean flag = this.save(knowledgeBase);

        return flag ? 1 : 0;

    }

    /**
     * 修改知识库
     *
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase) {
        return updateById(knowledgeBase) ? 1 : 0;
    }

    /**
     * 批量删除知识库
     *
     * @param ids 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseByIds(Long[] ids)
    {
        // 查询所有知识库
        List<KnowledgeBase> knowledgeBaseList = this.listByIds(Arrays.asList(ids));
        if(CollUtil.isEmpty(knowledgeBaseList)) {
            return 0;
        }

        for (KnowledgeBase knowledgeBase : knowledgeBaseList) {
            // 删除OSS的数据
            String documentUrl = knowledgeBase.getDocumentUrl();
            ossAliyunFileStorageService.delete(documentUrl);

            // 删除向量库的数据
            String idsJSON = knowledgeBase.getRemark();
            List<String> idList = JSONUtil.toList(idsJSON, String.class);
            vectorStore.delete(idList);
        }

        // 删除数据库的数据
        boolean result = this.removeByIds(Arrays.asList(ids));
        return result ? 1 :0;
    }

    /**
     * 删除知识库信息
     *
     * @param id 知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseById(Long id) {
        return removeById(id) ? 1 : 0;
    }
}
