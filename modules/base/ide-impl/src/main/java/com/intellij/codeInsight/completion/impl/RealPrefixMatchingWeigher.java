package com.intellij.codeInsight.completion.impl;

import consulo.language.editor.completion.PrefixMatcher;
import consulo.language.editor.completion.LookupElement;
import consulo.language.editor.completion.LookupElementWeigher;
import consulo.language.editor.completion.WeighingContext;
import javax.annotation.Nonnull;

/**
 * @author Peter
 */
public class RealPrefixMatchingWeigher extends LookupElementWeigher {

  public RealPrefixMatchingWeigher() {
    super("prefix", false, true);
  }

  @Override
  public Comparable weigh(@Nonnull LookupElement element, @Nonnull WeighingContext context) {
    return getBestMatchingDegree(element, CompletionServiceImpl.getItemMatcher(element, context));
  }

  public static int getBestMatchingDegree(LookupElement element, PrefixMatcher matcher) {
    int max = Integer.MIN_VALUE;
    for (String lookupString : element.getAllLookupStrings()) {
      max = Math.max(max, matcher.matchingDegree(lookupString));
    }
    return -max;
  }
}
