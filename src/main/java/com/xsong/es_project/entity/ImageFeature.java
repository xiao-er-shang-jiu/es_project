package com.xsong.es_project.entity;

import lombok.Data;

import java.util.List;

@Data
public class ImageFeature {

    private String id;

    private String name;

    private List image_vector;

    private String image;

    private Double score;
}
