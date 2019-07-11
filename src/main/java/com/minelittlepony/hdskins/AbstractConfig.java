package com.minelittlepony.hdskins;

import com.google.gson.annotations.Expose;

public abstract class AbstractConfig {

    @Expose
    public String lastChosenFile = "";
    
    public abstract void save();
}
