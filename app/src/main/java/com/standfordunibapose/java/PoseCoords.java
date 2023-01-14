package com.standfordunibapose.java;

public class PoseCoords {
    float x,y,z;
    int landmark;
    public PoseCoords(float x, float y, float z, int land){
        this.x = x;
        this.y = y;
        this.z = z;
        this.landmark = land;
    }
}
