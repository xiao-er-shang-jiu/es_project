package com.xsong.es_project.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.xsong.es_project.entity.ImageFeature;
import com.xsong.es_project.service.EsSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsSearchServiceImpl implements EsSearchService {

    @Value("${es.pyPath}")
    private String pyPath;

    @Value("${es.imagePath}")
    private String imagePath;

    @Value("${es.queryVectorTemplate}")
    private String queryVectorTemplate;

    @Value("${es.queryVectorKnnTemplate}")
    private String queryVectorKnnTemplate;

    private static final String EXE="python";

    private static final String[] FEATURE_KEY={"feature_1500","feature_512"};

    private Logger LOG= LoggerFactory.getLogger(EsSearchServiceImpl.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Override
    public List<ImageFeature> searchByFeature(Map<String,Object> params,String queryTemplate,String index) {
        List<ImageFeature> imageFeatureList=new ArrayList<>();
        FileReader fileReader=new FileReader(queryTemplate);
        String template=fileReader.readString();
        String bodyStr = StrUtil.format(template, params);
        Reader body=new StringReader(bodyStr);
        SearchRequest searchRequest=SearchRequest.of(b->b
                .index(index)
                .withJson(body)
        );
        SearchResponse<ImageFeature> response = null;
        try {
            response = elasticsearchClient.search(searchRequest, ImageFeature.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Hit<ImageFeature>> hits = response.hits().hits();
        for(Hit<ImageFeature> hit:hits){
            ImageFeature imageFeature=hit.source();
            imageFeature.setId(hit.id());
            imageFeature.setScore(hit.score());
            imageFeatureList.add(imageFeature);
        }
        return imageFeatureList;
    }

    @Override
    public List<ImageFeature> searchByFeatureExact(Map<String, Object> payload) {
        Map<String,Object> params=new HashMap<String,Object>(){{
            put("searchSize",payload.get("searchSize"));
            put("queryVector",payload.get("queryVector"));
        }};
        return searchByFeature(params,queryVectorTemplate,"es_img05");
    }

    @Override
    public List<ImageFeature> searchByFeatureKNN(Map<String, Object> payload) {
        Map<String,Object> params=new HashMap<String,Object>(){{
            put("k",payload.get("searchSize"));
            put("queryVector",payload.get("queryVector"));
        }};
        List<ImageFeature> imageFeatureList = searchByFeature(params, queryVectorKnnTemplate, "img05_test_02");
        System.out.println(imageFeatureList);
        for(ImageFeature imageFeature : imageFeatureList){
            String imageName=imageFeature.getName();
            try {
                Image image= ImageIO.read(new File(imagePath+imageName));
                String imageSuffix=imageName.split("\\.")[1];
                String base64= ImgUtil.toBase64DataUri(image,imageSuffix);
                imageFeature.setImage(base64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return imageFeatureList;
    }

    @Override
    public Map<String,String> extractFeature(String imagePath) {
        Map<String,String> featureList=new HashMap<>();
        String params="--img "+imagePath;
        String condaShell="conda activate isc21";
        String shell=StrUtil.format("{} {} {}",EXE,pyPath,params);
        RuntimeUtil.execForStr(condaShell);
        String str = RuntimeUtil.execForStr(shell);
        LOG.info("execute: "+shell);
        String vectorRegEx="(?<=\\[\\[).*?(?=\\]\\])";
        List<String> resultFindAll = ReUtil.findAll(vectorRegEx, str, 0, new ArrayList<String>());
        for(int i=0;i<resultFindAll.size();i++){
            String regEx="\\s+";
            String vector=resultFindAll.get(i);
            String feature=vector.trim().replaceAll(regEx,",");
            feature="["+feature+"]";
            featureList.put(FEATURE_KEY[i],feature);
            LOG.info("result: "+feature);
            LOG.info("dimension:"+feature.split(",").length);
        }
        return featureList;
    }
}
