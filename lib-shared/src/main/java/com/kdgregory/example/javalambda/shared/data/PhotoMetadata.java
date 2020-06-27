// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.sf.kdgcommons.lang.ObjectUtil;
import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.dynamodbv2.document.Item;


/**
 *  Holds information about the photo.
 *  <p>
 *  Instances may be created either from a map of simple data posted by the
 *  client, or from a map of <code>AttributeValue</code> objects retrieved
 *  from Dynamo. Since the two maps erase to the same type, construction uses
 *  the factory methods {@link #fromClientMap} and {@link #fromDynamoMap}.
 *  <p>
 *  By default, instances are ordered by newest upload date first.
 */
public class PhotoMetadata
implements Comparable<PhotoMetadata>
{
    /**
     *  Identifies all of the metadata fields. This is used both for Dynamo attribute
     *  names and client interaction.
     */
    public class Fields
    {
        public final static String  ID          = "id";
        public final static String  USERNAME    = "username";
        public final static String  FILENAME    = "filename";
        public final static String  DESCRIPTION = "description";
        public final static String  MIMETYPE    = "mimetype";
        public final static String  UPLOADED_AT = "uploadedAt";
        public final static String  SIZES       = "sizes";
    }

//----------------------------------------------------------------------------
//  Instance
//----------------------------------------------------------------------------

    private String id;
    private String user;
    private String filename;
    private String mimetype;
    private String description;
    private Long uploadedAt;
    private EnumSet<Sizes> sizes;


    public PhotoMetadata(String id, String user, String filename, String mimeType, String description, Long uploadedAt, Collection<String> sizes)
    {
        this.id = id;
        this.user = user;
        this.filename = filename;
        this.mimetype = mimeType;
        this.description = ObjectUtil.defaultValue(description, "");
        this.uploadedAt = uploadedAt;

        this.sizes = EnumSet.noneOf(Sizes.class);
        for (String sizeStr : sizes)
        {
            // an incorrect value is a program error so let it throw
            this.sizes.add(Sizes.valueOf(sizeStr));
        }
    }


    /**
     *  Constructs an instance using the map provided by a client upload. Note
     *  that we insert fields, and that instance sizes are left empty (to be
     *  filled by resizer).
     */
    public static PhotoMetadata fromClientMap(Map<String,Object> map)
    {
        return new PhotoMetadata(
            UUID.randomUUID().toString(),
            (String)map.get(Fields.USERNAME),
            (String)map.get(Fields.FILENAME),
            (String)map.get(Fields.MIMETYPE),
            (String)map.get(Fields.DESCRIPTION),
            System.currentTimeMillis(),
            Arrays.asList());
    }


    /**
     *  Constructs an instance using a DynamoDB Item (from the Document interface).
     */
    public static PhotoMetadata fromDynamoItem(Item item)
    {
        return new PhotoMetadata(
            item.getString(Fields.ID),
            item.getString(Fields.USERNAME),
            item.getString(Fields.FILENAME),
            item.getString(Fields.MIMETYPE),
            item.getString(Fields.DESCRIPTION),
            item.getLong(Fields.UPLOADED_AT),
            ObjectUtil.defaultValue(item.getStringSet(Fields.SIZES), Collections.emptySet()));
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
     *  Constructs a map suitable for returning to the client. In this form,
     *  the array of sizes has been expanded out to include all details.
     */
    public Map<String,Object> toClientMap()
    {
        Map<String,Object> result = new HashMap<String,Object>();

        result.put(Fields.ID,           id);
        result.put(Fields.USERNAME,     user);
        result.put(Fields.FILENAME,     filename);
        result.put(Fields.DESCRIPTION,  description);
        result.put(Fields.UPLOADED_AT,  uploadedAt);
        result.put(Fields.MIMETYPE,     mimetype);
        result.put(Fields.SIZES,        sizes.stream().map(Sizes::toMap)
                                             .collect(Collectors.toList()));

        return result;
    }


    /**
     *  Constructs a DynamoDB Item for upload.
     */
    public Item toDynamoItem()
    {
        Item item = new Item()
                   .withString(Fields.ID,           id)
                   .withString(Fields.USERNAME,     user)
                   .withString(Fields.FILENAME,     filename)
                   .withString(Fields.MIMETYPE,     mimetype)
                   .withLong(Fields.UPLOADED_AT,    uploadedAt);

        if (! StringUtil.isBlank(description))
        {
            item.withString(Fields.DESCRIPTION, description);
        }

        Set<String> sizeStrings = sizes.stream().map(Sizes::name).collect(Collectors.toSet());
        if (! sizeStrings.isEmpty())
        {
            item.withStringSet(Fields.SIZES, sizeStrings);
        }

        return item;
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


    @Override
    public int compareTo(PhotoMetadata that)
    {
        Long u1 = ObjectUtil.defaultValue(getUploadedAt(), Long.valueOf(Long.MAX_VALUE));
        Long u2 = ObjectUtil.defaultValue(that.getUploadedAt(), Long.valueOf(Long.MAX_VALUE));
        return -u1.compareTo(u2);
    }


    @Override
    public String toString()
    {
        return getClass().getSimpleName()
             + "[" + id + ": "
             + "username = " + user + ", "
             + "filename = " + filename + ", "
             + "mimetype = " + mimetype + ", "
             + "uploadedAt = " + uploadedAt + ", "
             + "sizes = " + sizes
             + "]";
    }
}
