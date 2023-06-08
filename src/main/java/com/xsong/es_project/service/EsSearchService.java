package com.xsong.es_project.service;

import com.xsong.es_project.entity.ImageFeature;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface EsSearchService {

    List<ImageFeature> searchByFeature(Map<String,Object> payload,String searchType,String index);

    List<ImageFeature> searchByFeatureExact(Map<String,Object> payload);

    List<ImageFeature> searchByFeatureKNN(Map<String,Object> payload);

    Map<String,String> extractFeature(String imagePath);
}
