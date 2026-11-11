--liquibase formatted sql
--changeset idea-support:category-guide-types-v1
ALTER TABLE categories ADD COLUMN guide_type VARCHAR(16) NOT NULL DEFAULT 'general';
UPDATE categories SET guide_type='food' WHERE id='aa88393f-142f-5803-a920-3ade6082bbb4';
UPDATE categories SET guide_type='software' WHERE id='2748d064-01f9-544b-a620-79db8a0f02cf';
UPDATE categories SET guide_type='contest' WHERE id='3f2b0879-b9cc-543b-8011-a6f543a949d6';
ALTER TABLE categories ADD CONSTRAINT ck_category_guide_type CHECK (guide_type IN ('food','software','contest','general'));
