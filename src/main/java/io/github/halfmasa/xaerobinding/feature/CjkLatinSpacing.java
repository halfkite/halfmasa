package io.github.halfmasa.xaerobinding.feature;

public final class CjkLatinSpacing
{
    private CjkLatinSpacing()
    {
    }

    public static String apply(String text)
    {
        return applyWithMapping(text).text();
    }

    public static Result applyWithMapping(String text)
    {
        if (text == null)
        {
            return new Result(null, new int[]{0}, new int[]{0});
        }

        StringBuilder result = new StringBuilder(text.length() + 8);
        int[] originalToSpaced = new int[text.length() + 1];
        int[] spacedToOriginal = new int[text.length() * 2 + 1];
        int originalIndex = 0;
        int spacedIndex = 0;
        int previous = -1;
        int index = 0;
        while (index < text.length())
        {
            int current = text.codePointAt(index);
            if (previous >= 0 && shouldSeparate(previous, current))
            {
                result.append(' ');
                spacedIndex++;
                spacedToOriginal[spacedIndex] = originalIndex;
                originalToSpaced[originalIndex] = spacedIndex;
            }
            result.appendCodePoint(current);
            int charCount = Character.charCount(current);
            for (int offset = 0; offset < charCount; offset++)
            {
                originalIndex++;
                spacedIndex++;
                originalToSpaced[originalIndex] = spacedIndex;
                spacedToOriginal[spacedIndex] = originalIndex;
            }
            previous = current;
            index += charCount;
        }

        return new Result(
                result.toString(),
                originalToSpaced,
                java.util.Arrays.copyOf(spacedToOriginal, spacedIndex + 1));
    }

    public static Result unchanged(String text)
    {
        if (text == null)
        {
            return new Result(null, new int[]{0}, new int[]{0});
        }
        int[] identity = new int[text.length() + 1];
        for (int index = 0; index < identity.length; index++)
        {
            identity[index] = index;
        }
        return new Result(text, identity, java.util.Arrays.copyOf(identity, identity.length));
    }

    public static boolean shouldSeparate(int left, int right)
    {
        return (isHan(left) && isLatinOrDigit(right)) ||
                (isLatinOrDigit(left) && isHan(right));
    }

    private static boolean isHan(int codePoint)
    {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private static boolean isLatinOrDigit(int codePoint)
    {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN ||
                Character.isDigit(codePoint);
    }

    public static final class Result
    {
        private final String text;
        private final int[] originalToSpaced;
        private final int[] spacedToOriginal;

        private Result(String text, int[] originalToSpaced, int[] spacedToOriginal)
        {
            this.text = text;
            this.originalToSpaced = originalToSpaced;
            this.spacedToOriginal = spacedToOriginal;
        }

        public String text()
        {
            return this.text;
        }

        public int toSpacedIndex(int originalIndex)
        {
            return this.originalToSpaced[clamp(originalIndex, this.originalToSpaced.length)];
        }

        public int toOriginalIndex(int spacedIndex)
        {
            return this.spacedToOriginal[clamp(spacedIndex, this.spacedToOriginal.length)];
        }

        private static int clamp(int index, int length)
        {
            return Math.max(0, Math.min(index, length - 1));
        }
    }
}
