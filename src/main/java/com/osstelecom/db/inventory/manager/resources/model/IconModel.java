package com.osstelecom.db.inventory.manager.resources.model;

import com.osstelecom.db.inventory.manager.request.BasicRequest;

public class IconModel{
   private String schemaName;
   private String mimeType;
   private String content;

    public String getSchemaName() {
        return schemaName;
    }
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public IconModel(String schemaName) {
        this.setSchemaName(schemaName);
    }
    public IconModel() {
    }
}
