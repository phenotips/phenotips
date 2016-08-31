/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.diff.DiffManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhenoTipsFeatureTest
{
    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private VocabularyTerm hp0000082;

    @Mock
    private VocabularyTerm hp0000100;

    @Mock
    private VocabularyTerm hp0003678;

    @Mock
    private VocabularyTerm hp0012211;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DiffManager.class)).thenReturn(null);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        when(this.hp0000082.getId()).thenReturn("HP:0000082");
        when(this.hp0000082.getName()).thenReturn("Decreased renal function");
        when(this.vm.resolveTerm("HP:0000082")).thenReturn(this.hp0000082);

        when(this.hp0000100.getId()).thenReturn("HP:0000100");
        when(this.hp0000100.getName()).thenReturn("Nephrosis");
        when(this.vm.resolveTerm("HP:0000100")).thenReturn(this.hp0000100);

        when(this.hp0003678.getId()).thenReturn("HP:0003678");
        when(this.hp0003678.getName()).thenReturn("Rapidly progressive");
        when(this.vm.resolveTerm("HP:0003678")).thenReturn(this.hp0003678);

        when(this.hp0012211.getId()).thenReturn("HP:0012211");
        when(this.hp0012211.getName()).thenReturn("Abnormal renal physiology");
        when(this.vm.resolveTerm("HP:0012211")).thenReturn(this.hp0012211);
    }

    @Test
    public void testPositivePropertiesNormalBehavior() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Some comments");
        temp = new StringProperty();
        temp.setValue("HP:0003678");
        temp.setName("pace_of_progression");
        when(meta.get("pace_of_progression")).thenReturn(temp);
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        List<BaseObject> categories = new ArrayList<>();
        BaseObject category = mock(BaseObject.class);
        temp = new StringProperty();
        temp.setValue("phenotype");
        when(category.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(category.get("target_property_value")).thenReturn(temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Assert.assertEquals("HP:0000100", f.getId());
        Assert.assertEquals("Nephrosis", f.getName());
        Assert.assertEquals("phenotype", f.getType());
        Assert.assertTrue(f.isPresent());
        Assert.assertEquals("Some comments", f.getNotes());

        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertEquals(1, metadata.size());
        FeatureMetadatum metadatum = metadata.get("pace_of_progression");
        Assert.assertNotNull(metadatum);
        Assert.assertEquals("HP:0003678", metadatum.getId());
        Assert.assertEquals("Rapidly progressive", metadatum.getName());
        Assert.assertEquals("pace_of_progression", metadatum.getType());

        // Testing the JSON format
        JSONObject json = f.toJSON();

        Assert.assertEquals("HP:0000100", json.getString("id"));
        Assert.assertEquals("Nephrosis", json.getString("label"));
        Assert.assertEquals("phenotype", json.getString("type"));
        Assert.assertEquals("yes", json.getString("observed"));
        Assert.assertEquals("Some comments", json.getString("notes"));

        JSONArray qualifiers = json.getJSONArray("qualifiers");
        Assert.assertEquals(1, qualifiers.length());
        JSONObject pop = qualifiers.getJSONObject(0);
        Assert.assertEquals("HP:0003678", pop.getString("id"));
        Assert.assertEquals("Rapidly progressive", pop.getString("label"));
        Assert.assertEquals("pace_of_progression", pop.getString("type"));

        JSONArray jsonCategories = json.getJSONArray("categories");
        Assert.assertEquals(2, jsonCategories.length());
        JSONObject categ = jsonCategories.getJSONObject(0);
        Assert.assertEquals("HP:0012211", categ.getString("id"));
        Assert.assertEquals("Abnormal renal physiology", categ.getString("label"));
        categ = jsonCategories.getJSONObject(1);
        Assert.assertEquals("HP:0000082", categ.getString("id"));
        Assert.assertEquals("Decreased renal function", categ.getString("label"));
    }

    @Test
    public void testNagativePropertiesNormalBehavior() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("negative_phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("negative_phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Some comments");
        temp = new StringProperty();
        temp.setValue("HP:0003678");
        temp.setName("pace_of_progression");
        when(meta.get("pace_of_progression")).thenReturn(temp);
        metas.add(meta);

        List<BaseObject> categories = new ArrayList<>();
        BaseObject category = mock(BaseObject.class);
        temp = new StringProperty();
        temp.setValue("negative_phenotype");
        when(category.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(category.get("target_property_value")).thenReturn(temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Assert.assertEquals("HP:0000100", f.getId());
        Assert.assertEquals("Nephrosis", f.getName());
        Assert.assertEquals("phenotype", f.getType());
        Assert.assertFalse(f.isPresent());
        Assert.assertEquals("Some comments", f.getNotes());

        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertEquals(1, metadata.size());
        FeatureMetadatum metadatum = metadata.get("pace_of_progression");
        Assert.assertNotNull(metadatum);
        Assert.assertEquals("HP:0003678", metadatum.getId());
        Assert.assertEquals("Rapidly progressive", metadatum.getName());
        Assert.assertEquals("pace_of_progression", metadatum.getType());

        // Testing the JSON format
        JSONObject json = f.toJSON();

        Assert.assertEquals("HP:0000100", json.getString("id"));
        Assert.assertEquals("Nephrosis", json.getString("label"));
        Assert.assertEquals("phenotype", json.getString("type"));
        Assert.assertEquals("no", json.getString("observed"));
        Assert.assertEquals("Some comments", json.getString("notes"));

        JSONArray qualifiers = json.getJSONArray("qualifiers");
        Assert.assertEquals(1, qualifiers.length());
        JSONObject pop = qualifiers.getJSONObject(0);
        Assert.assertEquals("HP:0003678", pop.getString("id"));
        Assert.assertEquals("Rapidly progressive", pop.getString("label"));
        Assert.assertEquals("pace_of_progression", pop.getString("type"));

        JSONArray jsonCategories = json.getJSONArray("categories");
        Assert.assertEquals(2, jsonCategories.length());
        JSONObject categ = jsonCategories.getJSONObject(0);
        Assert.assertEquals("HP:0012211", categ.getString("id"));
        Assert.assertEquals("Abnormal renal physiology", categ.getString("label"));
        categ = jsonCategories.getJSONObject(1);
        Assert.assertEquals("HP:0000082", categ.getString("id"));
        Assert.assertEquals("Decreased renal function", categ.getString("label"));
    }

    @Test
    public void nullMetadataObjectsDontCauseErrors() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        metas.add(null);

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Some comments");
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Assert.assertEquals("Some comments", f.getNotes());

        JSONObject json = f.toJSON();
        Assert.assertEquals("Some comments", json.getString("notes"));
    }

    @Test
    public void propertyNameIsCheckedWhenSearchingMetadata() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("prenatal_phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Wrong comments");
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Assert.assertEquals("", f.getNotes());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("notes"));
    }

    @Test
    public void propertyValueIsCheckedWhenSearchingMetadata() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000123");
        when(meta.get("target_property_value")).thenReturn(temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Wrong comments");
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Assert.assertEquals("", f.getNotes());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("notes"));
    }

    @Test
    public void emptyQualifiersAreDiscarded() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("");
        temp.setName("pace_of_progression");
        when(meta.get("pace_of_progression")).thenReturn(temp);
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertTrue(metadata.isEmpty());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("qualifiers"));
    }

    @Test
    public void missingQualifiersAreDiscarded() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();

        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue(null);
        temp.setName("pace_of_progression");
        when(meta.get("pace_of_progression")).thenReturn(temp);
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertTrue(metadata.isEmpty());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("qualifiers"));
    }

    @Test
    public void noQualifiersWithoutMetadataObjects() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(Collections.<BaseObject>emptyList());

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertTrue(metadata.isEmpty());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("qualifiers"));
    }

    @Test
    public void noQualifiersWithNullMetadataList() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");

        // Not really expected, but we should consider this case anyway
        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(null);

        Feature f = new PhenoTipsFeature(doc, prop, "HP:0000100");
        Map<String, ? extends FeatureMetadatum> metadata = f.getMetadata();
        Assert.assertTrue(metadata.isEmpty());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("qualifiers"));
    }

    @Test
    public void nullXPropertiesDontCauseErrorsWhenSearchingMetadata() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> metas = new ArrayList<>();
        BaseObject meta = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(meta.get("target_property_name")).thenReturn(null, null, null, temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(meta.get("target_property_value")).thenReturn(null, temp, null, temp);
        when(meta.getLargeStringValue("comments")).thenReturn("Some comments");
        metas.add(meta);

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(metas);

        Assert.assertEquals("", new PhenoTipsFeature(doc, prop, "HP:0000100").getNotes());
        Assert.assertEquals("", new PhenoTipsFeature(doc, prop, "HP:0000100").getNotes());
        Assert.assertEquals("", new PhenoTipsFeature(doc, prop, "HP:0000100").getNotes());
        Assert.assertEquals("Some comments", new PhenoTipsFeature(doc, prop, "HP:0000100").getNotes());
    }

    @Test
    public void customTermsOnlyUseTheLabel() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");

        when(doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE)).thenReturn(Collections.<BaseObject>emptyList());

        Feature f = new PhenoTipsFeature(doc, prop, "Tetrapyloctomist");
        Assert.assertEquals("", f.getId());
        Assert.assertEquals("Tetrapyloctomist", f.getName());
        Assert.assertEquals("Tetrapyloctomist", f.getValue());

        JSONObject json = f.toJSON();
        Assert.assertFalse(json.has("id"));
        Assert.assertEquals("Tetrapyloctomist", json.getString("label"));
    }

    @Test
    public void nullCategoryObjectsDontCauseErrors() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> categories = new ArrayList<>();
        categories.add(null);
        BaseObject category = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(category.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(category.get("target_property_value")).thenReturn(temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        JSONObject json = new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON();
        JSONArray jsonCategories = json.getJSONArray("categories");
        Assert.assertEquals(2, jsonCategories.length());
    }

    @Test
    public void propertyNameIsCheckedWhenSearchingCategories() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> categories = new ArrayList<>();
        BaseObject category = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("prenatal_phenotype");
        when(category.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(category.get("target_property_value")).thenReturn(temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        JSONObject json = new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON();
        Assert.assertFalse(json.has("categories"));
    }

    @Test
    public void propertyValueIsCheckedWhenSearchingCategories() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> categories = new ArrayList<>();
        BaseObject category = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(category.get("target_property_name")).thenReturn(temp);
        temp = new StringProperty();
        temp.setValue("HP:0000123");
        when(category.get("target_property_value")).thenReturn(temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        JSONObject json = new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON();
        Assert.assertFalse(json.has("categories"));
    }

    @Test
    public void nullXPropertiesDontCauseErrorsWhenSearchingCategories() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");
        List<BaseObject> categories = new ArrayList<>();
        BaseObject category = mock(BaseObject.class);
        StringProperty temp = new StringProperty();
        temp.setValue("phenotype");
        when(category.get("target_property_name")).thenReturn(null, null, null, temp);
        temp = new StringProperty();
        temp.setValue("HP:0000100");
        when(category.get("target_property_value")).thenReturn(null, temp, null, temp);
        List<String> specifiedCategories = new ArrayList<>();
        specifiedCategories.add("HP:0012211");
        specifiedCategories.add("HP:0000082");
        when(category.getListValue("target_property_category")).thenReturn(null, specifiedCategories);
        categories.add(category);
        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(categories);

        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
        Assert.assertTrue(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
    }

    @Test
    public void noCategoriesWithoutObjects() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");

        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(Collections.<BaseObject>emptyList());

        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
    }

    @Test
    public void noCategoriesWithNullObjectList() throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("phenotype");

        when(doc.getXObjects(new EntityReference("PhenotypeCategoryClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE))).thenReturn(null);

        Assert.assertFalse(new PhenoTipsFeature(doc, prop, "HP:0000100").toJSON().has("categories"));
    }
}
