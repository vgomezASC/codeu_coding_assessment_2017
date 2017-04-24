// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codeu.codingchallenge;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MyJSONParser implements JSONParser {

  @Override
  public JSON parse(String in) throws IOException {
    try {
      in.trim();
      if (in.charAt(0) != '{' || in.charAt(in.length() - 1) != '}')
        throw new SyntaxException("Braces not found");

      JSON object = new MyJSON();
      int i = 1;
      String key = null;
      boolean expectingComma = false, expectingColon = false;
      Set<String> usedKeys = new TreeSet<String>();
      while (i < in.length()) {
        switch (in.charAt(i)) {
        case ' ':
        case '\r':
        case '\n':
        case '\t':
          Debug.println(i + ": Whitespace");
          // Let it slide, it's just whitespace.
          break;

        case '"':
          Debug.println(i + ": Quote");
          if (expectingComma || expectingColon)
            throw new SyntaxException();

          // Parse the string.
          Pattern endOfStringPattern = Pattern.compile("[^\\\\]\"");
          Matcher endOfStringMatcher = endOfStringPattern.matcher(in);
          if (!endOfStringMatcher.find(i + 1)) {
            throw new SyntaxException("No end of string found");
          }

          int endOfString = endOfStringMatcher.end() - 1;
          Debug.println(i + " - " + endOfString + ": String range");

          String str = in.substring(i + 1, endOfString);
          Debug.println("String: " + str);

          Pattern unescapedQuotePattern = Pattern.compile("[^\\\\]\"|^\"");
          Matcher strQuoteMatcher = unescapedQuotePattern.matcher(str);
          if (strQuoteMatcher.matches()) {
            throw new SyntaxException("Unescaped quote inside string");
          }

          String parsedString = parseEscapeSequences(str);
          Debug.println("Parsed string: " + parsedString);

          if (key == null) {
            // The string we just parsed was a key.
            // Expect a colon to come later.
            Debug.println("String is a key.");
            if (usedKeys.add(parsedString)) {
              key = parsedString;
              expectingColon = true;
            } else {
              // Key already exists.
              throw new SyntaxException();
            }
          } else {
            Debug.println("String is a value.");
            // The string we just parsed was a value.
            // Expect a comma to come later.
            object.setString(key, parsedString);
            key = null;
            expectingComma = true;
          }
          i = endOfString;
          Debug.println("Jumping to: " + i);
          break;

        case ':':
          Debug.println(i + ": Colon");
          // Only valid if we are expecting it (i.e. a key was just defined).
          if (!expectingColon) {
            throw new SyntaxException();
          }
          assert (key != null);
          expectingColon = false;
          break;

        case ',':
          Debug.println(i + ": Comma");
          // Only valid if we are expecting it (i.e. a key-value pair was just
          // defined).
          if (!expectingComma) {
            throw new SyntaxException();
          }
          assert (key == null);
          expectingComma = false;
          break;

        case '{':
          Debug.println(i + ": Start object");
          if (expectingComma || expectingColon || key == null)
            throw new SyntaxException();

          // Parse the object.
          int j = i + 1, depth = 1;
          boolean insideQuotes = false, escaping = false;
          while (true) {
            // Go inside the object and find our end curly brace.
            // Don't get caught by braces inside strings, though!
            if (!escaping) {
              if (in.charAt(j) == '"')
                if (insideQuotes)
                  insideQuotes = false;
                else
                  insideQuotes = true;
              else if (in.charAt(j) == '\\')
                escaping = true;
              else if (in.charAt(j) == '{' && !insideQuotes)
                depth++;
              else if (in.charAt(j) == '}' && !insideQuotes && --depth == 0)
                break;
            } else {
              escaping = false;
            }
            if (++j > in.length()) {
              throw new SyntaxException("Expected end of object");
            }
          }
          assert (j != i);

          Debug.println(i + " - " + j + ": Object range");

          String innerObjectString = in.substring(i, j + 1);

          Debug.println("Inner object: " + innerObjectString);
          JSON innerObject = parse(innerObjectString);
          object.setObject(key, innerObject);

          key = null;
          expectingComma = true;

          i = j;
          break;

        case '}':
          Debug.println(i + ": End object");
          if (!expectingComma && !usedKeys.isEmpty()) {
            // The parser ran through a comma just previously.
            // But the object might actually just be an empty object.
            throw new SyntaxException("Ending object with trailing comma");
          }
          if (expectingColon) {
            throw new SyntaxException("Incomplete declaration of element");
          }
          assert (key == null);
          assert (i == in.length());
          expectingComma = false;
          break;

        default:
          throw new SyntaxException("Unexpected character");
        }
        i++;
        Debug.println("Going to " + i);
      }
      if(expectingComma) {
        throw new SyntaxException("Expected end of object");
      }
      
      return object;
    } catch (Exception e) {
      // Whatever exception we get, throw it as an IOException to meet the
      // guidelines.
      throw new IOException(e);
    }
  }

  private String parseEscapeSequences(String in) throws SyntaxException {
    String parsed = new String(in);

    // There's a regex solution, but after about an hour, I just couldn't figure
    // it out.
    boolean escaped = false;
    for (int i = 0; i < parsed.length(); i++) {
      char c = parsed.charAt(i);
      if (escaped)
        if (c == '\\' || c == '"' || c == 'n' || c == 't')
          escaped = false;
        else
          throw new SyntaxException("Invalid/unsupported escape character");
      else if (c == '\\')
        escaped = true;
    }

    parsed = parsed.replace("\\t", "\t");
    parsed = parsed.replace("\\n", "\n");
    parsed = parsed.replace("\\\"", "\"");

    return parsed;
  }

  final class SyntaxException extends Exception {
    private static final long serialVersionUID = 1L;

    public SyntaxException() {
    }

    public SyntaxException(String message) {
      super("Syntax error: " + message);
    }
  }
}
