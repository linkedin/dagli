package com.linkedin.dagli.avro;

import com.linkedin.dagli.annotation.struct.Struct;


@SuppressWarnings("all")
/** Avro schema for testing Dagli Avro support */
@Struct("TestAvroSchemaStruct") // create this for testing purposes
public class TestAvroSchema extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1;

  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"TestAvroSchema\",\"namespace\":\"com.linkedin.dagli.data.test\",\"fields\":[{\"name\":\"created\",\"type\":\"long\",\"doc\":\"When this record was created\"},{\"name\":\"edited\",\"type\":[\"null\",\"long\"],\"doc\":\"When this record was edited\",\"default\":null},{\"name\":\"userName\",\"type\":\"string\",\"doc\":\"The name of the user\"},{\"name\":\"recursion\",\"type\":[\"null\",\"TestAvroSchema\"],\"doc\":\"The a recursive instance of this record type\",\"default\":null}]}");
  /** When this record was created */
  public long created;
  /** When this record was edited */
  public java.lang.Long edited;
  /** The name of the user */
  public java.lang.CharSequence userName;
  /** The a recursive instance of this record type */
  public TestAvroSchema recursion;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return created;
    case 1: return edited;
    case 2: return userName;
    case 3: return recursion;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: created = (java.lang.Long)value$; break;
    case 1: edited = (java.lang.Long)value$; break;
    case 2: userName = (java.lang.CharSequence)value$; break;
    case 3: recursion = (TestAvroSchema)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
