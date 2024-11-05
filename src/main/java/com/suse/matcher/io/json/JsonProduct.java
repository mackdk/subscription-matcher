/**
 * Copyright (c) 2016 SUSE LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *    * Neither the name of SUSE LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.suse.matcher.io.json;

/**
 * JSON representation of a product.
 */
public class JsonProduct {

    /** The id. */
    private Long id;

    /** The friendly name. */
    private String name;

    /** The product class */
    private String productClass;

    /** true if this is a free product. */
    private Boolean free;

    /** true if this is a base product. */
    private Boolean base;

    /**
     * Standard constructor.
     *
     * @param idIn the id
     * @param nameIn the name
     * @param productClassIn the productClass
     * @param freeIn true if this is a free product
     * @param baseIn true if this is a base product
     */
    public JsonProduct(Long idIn, String nameIn, String productClassIn, Boolean freeIn, Boolean baseIn) {
        id = idIn;
        name = nameIn;
        productClass = productClassIn;
        free = freeIn;
        base = baseIn;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param idIn the new id
     */
    public void setId(Long idIn) {
        id = idIn;
    }

    /**
     * Gets the friendly name.
     *
     * @return the friendly name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the friendly name.
     *
     * @param nameIn the new friendly name
     */
    public void setName(String nameIn) {
        name = nameIn;
    }

    /**
     * @return the productClass
     */
    public String getProductClass() {
        return productClass;
    }

    /**
     * @param productClassIn the productClass to set
     */
    public void setProductClass(String productClassIn) {
        this.productClass = productClassIn;
    }

    /**
     * Checks if the product is free.
     *
     * @return true if this is a free product
     */
    public Boolean getFree() {
        return free;
    }

    /**
     * Changes whether this is a free product or not.
     *
     * @param freeIn true if this is a free product
     */
    public void setFree(Boolean freeIn) {
        free = freeIn;
    }

    /**
     * Returns true if this is a base product.
     *
     * @return true if this is a base product
     */
    public Boolean getBase() {
        return base;
    }

    /**
     * Set to true if this is a base product.
     *
     * @param baseIn true if this is a base product
     */
    public void setBase(Boolean baseIn) {
        base = baseIn;
    }
}
