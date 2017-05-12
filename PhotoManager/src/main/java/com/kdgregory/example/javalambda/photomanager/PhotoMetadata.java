// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.kdgcommons.collections.CollectionUtil;
import net.sf.kdgcommons.lang.ObjectUtil;
import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;


/**
 *  Holds information about the photo.
 *  <p>
 *  Instances may be created either from a map of simple data posted by the
 *  client, or from a map of <code>AttributeValue</code> objects retrieved
 *  from Dynamo. Since the two maps erase to the same type, construction uses
 *  the factory methods {@link #fromClientMap} and {@link #fromDynamoMap}.
 */
public class PhotoMetadata
{
    /**
     *  Names for all of the fields in this object. These will be used as map keys.
     */
    public static class Fields
    {
        public final static String  ID          = "id";
        public final static String  USER        = "user";
        public final static String  FILENAME    = "filename";
        public final static String  DESCRIPTION = "description";
        public final static String  MIMETYPE    = "mimetype";
        public final static String  UPLOADED_AT = "uploadedAt";
        public final static String  SIZES       = "sizes";
    }

    final public static List<String> ALL_FIELDS = Arrays.asList(
        Fields.ID, Fields.USER, Fields.FILENAME, Fields.DESCRIPTION, Fields.MIMETYPE, Fields.UPLOADED_AT, Fields.SIZES);


    /**
     *  Defines all supported sizes, along with information about those sizes.
     *  The enum name is used in the S3 key for the image.
     */
    public enum Sizes
    {
        FULLSIZE
    }

//----------------------------------------------------------------------------
//  Fields, constructor, and static factory methods
//----------------------------------------------------------------------------

    private String id;
    private String user;
    private String filename;
    private String mimetype;
    private String description;
    private Long uploadedAt;
    private Set<Sizes> sizes;


    /**
     *  Single constructor: either we get a map of simple objects from the
     *  client, or a map of AttributeValue objects from Dynamo. Unfortunately
     *  there's no way to
     */
    private PhotoMetadata(String id, String user, String filename, String mimeType, String description, Long uploadedAt, Collection<String> sizes)
    {
        this.id = id;
        this.user = user;
        this.filename = filename;
        this.mimetype = mimeType;
        this.description = ObjectUtil.defaultValue(description, "");
        this.uploadedAt = uploadedAt;
        this.sizes = parseSizes(sizes);
    }


    /**
     *  Constructs an instance using the map provided by a client POST.
     */
    public static PhotoMetadata fromClientMap(Map<String,Object> map)
    {
        return new PhotoMetadata(
            (String)map.get(Fields.ID),
            (String)map.get(Fields.USER),
            (String)map.get(Fields.FILENAME),
            (String)map.get(Fields.MIMETYPE),
            (String)map.get(Fields.DESCRIPTION),
            (Long)map.get(Fields.UPLOADED_AT),
            Arrays.asList(Sizes.FULLSIZE.name()));
    }


    /**
     *  Constructs an instance using the map provided by a DynamoDB scan.
     */
    public static PhotoMetadata fromDynamoMap(Map<String,AttributeValue> map)
    {
        return new PhotoMetadata(
            map.get(Fields.ID).getS(),
            map.get(Fields.USER).getS(),
            map.get(Fields.FILENAME).getS(),
            map.get(Fields.MIMETYPE).getS(),
            map.get(Fields.DESCRIPTION).getS(),
            map.containsKey(Fields.UPLOADED_AT)
                        ? Long.valueOf(map.get(Fields.UPLOADED_AT).getN())
                        : null,
            map.get(Fields.SIZES).getSS());
    }


//----------------------------------------------------------------------------
//  Accessors -- read-only
//----------------------------------------------------------------------------

    public String getId()
    {
        return id;
    }


    public String getUser()
    {
        return user;
    }


    public String getFilename()
    {
        return filename;
    }


    public String getDescription()
    {
        return description;
    }


    public String getMimetype()
    {
        return mimetype;
    }


    public Long getUploadedAt()
    {
        return uploadedAt;
    }


    public Set<Sizes> getSizes()
    {
        return sizes;
    }


//----------------------------------------------------------------------------
//  Other public methods
//----------------------------------------------------------------------------

    /**
     *  Constructs a map suitable for returning to the client.
     */
    public Map<String,Object> toClientMap()
    {
        Map<String,Object> result = new HashMap<String,Object>();

        result.put(Fields.ID,           id);
        result.put(Fields.USER,         user);
        result.put(Fields.FILENAME,     filename);
        result.put(Fields.DESCRIPTION,  description);
        result.put(Fields.UPLOADED_AT,  uploadedAt);
        result.put(Fields.MIMETYPE,     mimetype);

        List<String> sizeStrings = new ArrayList<String>(sizes.size());
        for (Sizes size : sizes)
        {
            sizeStrings.add(size.name());
        }
        result.put(Fields.SIZES, sizeStrings);

        return result;
    }


    /**
     *  Constructs a map suitable for upload to Dynamo.
     */
    public Map<String,AttributeValue> toDynamoMap()
    {
        Map<String,AttributeValue> result = new HashMap<String,AttributeValue>();

        if (! StringUtil.isBlank(id))           result.put(Fields.ID,           new AttributeValue().withS(id));
        if (! StringUtil.isBlank(user))         result.put(Fields.USER,         new AttributeValue().withS(user));
        if (! StringUtil.isBlank(filename))     result.put(Fields.FILENAME,     new AttributeValue().withS(filename));
        if (! StringUtil.isBlank(mimetype))     result.put(Fields.MIMETYPE,     new AttributeValue().withS(mimetype));
        if (! StringUtil.isBlank(description))  result.put(Fields.DESCRIPTION,  new AttributeValue().withS(description));
        if (uploadedAt != null)                 result.put(Fields.UPLOADED_AT,  new AttributeValue().withN(uploadedAt.toString()));
        if (! CollectionUtil.isEmpty(sizes))    result.put(Fields.SIZES,        new AttributeValue().withSS(stringifySizes(sizes)));

        return result;
    }


    /**
     *  Determines whether this is a valid object: whether it has enough data
     *  to be stored and later retrieved.
     */
    public boolean isValid()
    {
        boolean fail  = StringUtil.isEmpty(id)
                     || StringUtil.isEmpty(filename)
                     || StringUtil.isEmpty(mimetype);

        return !fail;
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Utility method to translate a collection of strings (which may be
     *  empty) into a set of sizes.
     */
    private static Set<Sizes> parseSizes(Collection<String> values)
    {
        Set<Sizes> result = EnumSet.noneOf(Sizes.class);

        if (CollectionUtil.isEmpty(values))
            return result;

        for (String sizeStr : values)
        {
            // an incorrect value is a program error so let it throw
            result.add(Sizes.valueOf(sizeStr));
        }
        return result;
    }


    /**
     *  Converts a collection of size enum values to their string version.
     */
    private static List<String> stringifySizes(Collection<Sizes> values)
    {
        List<String> result = new ArrayList<String>();

        if (CollectionUtil.isEmpty(values))
            return result;

        for (Sizes item : values)
        {
            result.add(item.name());
        }

        return result;
    }
}
