package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BoardJsonConverter implements AttributeConverter<Board, String> {

    @Override
    public String convertToDatabaseColumn(Board attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(attribute);
            }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize board.", exception);
        }
    }

    @Override
    public Board convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            byte[] serializedBoard = Base64.getDecoder().decode(dbData);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serializedBoard))) {
                return (Board) objectInputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not deserialize board.", exception);
        }
    }
}