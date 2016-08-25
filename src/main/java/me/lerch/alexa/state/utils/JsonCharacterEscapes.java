package me.lerch.alexa.state.utils;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class JsonCharacterEscapes extends CharacterEscapes {
    @Override
    public int[] getEscapeCodesForAscii() {
        return CharacterEscapes.standardAsciiEscapesForJSON();
    }

    @Override
    public SerializableString getEscapeSequence(int i) {
        return new SerializedString("");
    }
}
