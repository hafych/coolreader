package org.coolreader.crengine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ParseBudgetTest {
	@Test
	public void stableCodesRoundTrip() {
		for (ParseBudget.Error error : ParseBudget.Error.values())
			assertEquals(error, ParseBudget.Error.fromCode(error.getCode()));
		assertNull(ParseBudget.Error.fromCode(Integer.MAX_VALUE));
	}

	@Test
	public void documentLimitAcceptsBoundaryAndRejectsOverflow() throws Exception {
		ParseBudget.requireDocumentBytes(ParseBudget.MAX_DOCUMENT_INPUT_BYTES);
		try {
			ParseBudget.requireDocumentBytes(ParseBudget.MAX_DOCUMENT_INPUT_BYTES + 1);
			fail("Document above the input budget was accepted");
		} catch (ParseBudget.LimitExceededException expected) {
			assertEquals(ParseBudget.Error.INPUT_BYTES, expected.getError());
			assertEquals(1001, expected.getError().getCode());
		}
	}

	@Test
	public void negativeKnownSizeIsRejected() {
		try {
			ParseBudget.requireDocumentBytes(-1);
			fail("Negative known document size was accepted");
		} catch (ParseBudget.LimitExceededException expected) {
			assertEquals("input-bytes", expected.getError().getWireName());
		}
	}
}
