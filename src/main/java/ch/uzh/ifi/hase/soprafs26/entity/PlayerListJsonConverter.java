package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PlayerListJsonConverter implements AttributeConverter<List<Player>, String> {

    @Override
    public String convertToDatabaseColumn(List<Player> attribute) {
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
            throw new IllegalStateException("Could not serialize players.", exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Player> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }

        try {
            byte[] serializedPlayers = Base64.getDecoder().decode(dbData);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serializedPlayers))) {
                return (List<Player>) objectInputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Could not deserialize players.", exception);
        }
    }
}
