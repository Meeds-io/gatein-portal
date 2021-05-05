package com.ibatis.jpetstore.domain;

import java.io.Serializable;

import org.jboss.portal.portlet.samples.util.SimpleHtmlExtractor;

public class Product implements Serializable {

    private String productId;
    private String categoryId;
    private String name;
    private String description;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId.trim();
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageSource() {
        String src = SimpleHtmlExtractor.extractAttribute(description, "src");
        if (src == null)
            return description;
        return src;
    }

    public String getCleanDescription() {
        return SimpleHtmlExtractor.removeElements(description);
    }

    public String toString() {
        return getName();
    }
}
