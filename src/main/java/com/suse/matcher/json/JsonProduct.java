package com.suse.matcher.json;

/**
 * JSON representation of a product.
 * 
 * @param id the product id
 * @param name the friendly name
 * @param productClass the product class
 * @param free {@code true} if this is a free product
 * @param base {@code true} if this is a base product
 */
public record JsonProduct(Long id, String name, String productClass, Boolean free, Boolean base) {
}
