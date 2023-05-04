package com.utilities.utils;

public class JsonDiffHolder {
    Object valueInJson1;
    Object valueInJson2;

    public JsonDiffHolder(Object valueInJson1, Object valueInJson2){
        this.valueInJson1 = valueInJson1;
        this.valueInJson2 = valueInJson2;
    }

    public Object getValueInJson1(){return this.valueInJson1;}
    public Object getValueInJson2(){return this.valueInJson2;}

    @Override
    public String toString(){
        return "(valueInJson1: "+this.valueInJson1+", valueInJson2: "+this.valueInJson2+")";
    }
}
