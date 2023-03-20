package com.osstelecom.db.inventory.manager.resources.model;

public class IconModel {
   private String schemaName;
   private String mimeType;
   private String conteudo;

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
    public String getConteudo() {
        return conteudo;
    }
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public IconModel(String schemaName) {
        this.setSchemaName(schemaName);
    }
    
}
