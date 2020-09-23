package com.linkedin.dagli.examples.fasttextandavro;

@SuppressWarnings("all")
/** Avro record for Shakespeare character dialog */
public class CharacterDialog extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"CharacterDialog\",\"namespace\":\"com.linkedin.dagli.examples.fasttext\",\"fields\":[{\"name\":\"character\",\"type\":\"string\",\"doc\":\"The name of the character saying something\"},{\"name\":\"dialog\",\"type\":\"string\",\"doc\":\"What the character says\"}]}");
  /** The name of the character saying something */
  public java.lang.CharSequence character;
  /** What the character says */
  public java.lang.CharSequence dialog;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return character;
    case 1: return dialog;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: character = (java.lang.CharSequence)value$; break;
    case 1: dialog = (java.lang.CharSequence)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
