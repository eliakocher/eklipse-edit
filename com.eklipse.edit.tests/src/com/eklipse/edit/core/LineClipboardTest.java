package com.eklipse.edit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.junit.jupiter.api.Test;

class LineClipboardTest {

	@Test
	void lineTextHasTheLineDelimiter() throws BadLocationException {
		Document document = new Document("first\nsecond\nthird\n");
		assertEquals("second\n", LineClipboard.lineText(document, 9));
		assertEquals("second\n", LineClipboard.lineText(document, 6), "at the start of the line");
		assertEquals("second\n", LineClipboard.lineText(document, 12), "at the end of the line");
	}

	@Test
	void lineTextKeepsWindowsDelimiters() throws BadLocationException {
		assertEquals("second\r\n", LineClipboard.lineText(new Document("first\r\nsecond\r\nthird"), 9));
	}

	@Test
	void lastLineGetsTheDefaultDelimiter() throws BadLocationException {
		assertEquals("third\r\n", LineClipboard.lineText(new Document("first\r\nthird"), 9));
		assertEquals("\n", LineClipboard.lineText(new Document("first\n"), 6), "the empty line after the last delimiter");
	}

	@Test
	void cutLineKeepsTheColumn() throws BadLocationException {
		Document document = new Document("first\nsecond\nthird\n");
		assertEquals(8, LineClipboard.cutLine(document, 8));
		assertEquals("first\nthird\n", document.get());
	}

	@Test
	void cutLineStopsAtTheEndOfAShorterLine() throws BadLocationException {
		Document document = new Document("a\nsecond\nb\n");
		assertEquals(3, LineClipboard.cutLine(document, 7));
		assertEquals("a\nb\n", document.get());
	}

	@Test
	void cutLastLineRemovesTheDelimiterBefore() throws BadLocationException {
		Document document = new Document("first\r\nsecond\r\nthird");
		assertEquals(10, LineClipboard.cutLine(document, 18));
		assertEquals("first\r\nsecond", document.get());
	}

	@Test
	void cutOnlyLine() throws BadLocationException {
		Document document = new Document("only");
		assertEquals(0, LineClipboard.cutLine(document, 2));
		assertEquals("", document.get());
	}

	@Test
	void cutEmptyLastLine() throws BadLocationException {
		Document document = new Document("first\n");
		assertEquals(0, LineClipboard.cutLine(document, 6));
		assertEquals("first", document.get());
	}

	@Test
	void pasteLineGoesAboveTheCurrentLine() throws BadLocationException {
		Document document = new Document("first\nsecond\n");
		assertEquals(15, LineClipboard.pasteLine(document, 8, "pasted\n"));
		assertEquals("first\npasted\nsecond\n", document.get());
	}

	@Test
	void pasteLineUsesTheDocumentsDelimiter() throws BadLocationException {
		Document document = new Document("first\r\nsecond");
		assertEquals(17, LineClipboard.pasteLine(document, 9, "pasted\n"));
		assertEquals("first\r\npasted\r\nsecond", document.get());
	}

	@Test
	void pasteLineOnTheLastLine() throws BadLocationException {
		Document document = new Document("first\nlast");
		assertEquals(17, LineClipboard.pasteLine(document, 10, "pasted"));
		assertEquals("first\npasted\nlast", document.get());
	}
}
