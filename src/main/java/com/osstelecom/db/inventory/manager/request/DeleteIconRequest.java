package com.osstelecom.db.inventory.manager.request;

import com.osstelecom.db.inventory.manager.resources.model.IconModel;

public class DeleteIconRequest extends BasicRequest<IconModel> {

    public DeleteIconRequest(String id) {
        this.setPayLoad(new IconModel(id));
    }

}
