package com.xsong.es_project.controller;

import cn.hutool.core.util.RandomUtil;
import com.xsong.es_project.entity.ImageFeature;
import com.xsong.es_project.service.EsSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/es")
public class ElasticsearchController {

    @Autowired
    private EsSearchService esSearchService;

    @Value("${es.uploadPath}")
    private String uploadPath;

    @RequestMapping("/uploadImage")
    public Map<String,String> uploadImage(MultipartFile file){
        Map<String,String> response=new HashMap<>();
        String oldName=file.getOriginalFilename();
        String newName=UUID.randomUUID().toString()+oldName.substring(oldName.lastIndexOf("."));
        String imagePath=uploadPath+newName;
        File image=new File(imagePath);
        try {
            file.transferTo(image);
            response=esSearchService.extractFeature(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @RequestMapping("/queryByFeatureExact")
    public List<ImageFeature> queryByFeature(@RequestBody Map<String,Object> payload){
        List<ImageFeature> imageFeatureList = esSearchService.searchByFeatureExact(payload);
        return imageFeatureList;
    }

    @RequestMapping("/queryByFeatureKNN")
    public List<ImageFeature> queryByFeatureKNN(@RequestBody Map<String,Object> payload){
        List<ImageFeature> imageFeatureList = esSearchService.searchByFeatureKNN(payload);
        return imageFeatureList;
    }
}
