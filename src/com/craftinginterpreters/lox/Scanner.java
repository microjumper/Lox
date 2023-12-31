package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner
{
	private static final Map<String, TokenType> keywords = new HashMap<String, TokenType>()
	{{
		put("and", AND);
		put("class", CLASS);
		put("else", ELSE);
		put("false", FALSE);
		put("for", FOR);
		put("fun", FUN);
		put("if", IF);
		put("nil", NIL);
		put("or", OR);
		put("print", PRINT);
		put("return", RETURN);
		put("super", SUPER);
		put("this", THIS);
		put("true", TRUE);
		put("var", VAR);
		put("while", WHILE);
	}};
	
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	
	Scanner(String source)
	{
		this.source = source;
	}
	
	List<Token> scanTokens()
	{
		while (!isAtEnd())
		{
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line));
		
		return tokens;
	}
	
	private boolean isAtEnd()
	{
		return current >= source.length();
	}
	
	private void scanToken()
	{
		char c = advance();
		switch (c)
		{
			case '(':
				addToken(LEFT_PAREN);
				break;
			case ')':
				addToken(RIGHT_PAREN);
				break;
			case '{':
				addToken(LEFT_BRACE);
				break;
			case '}':
				addToken(RIGHT_BRACE);
				break;
			case ',':
				addToken(COMMA);
				break;
			case '.':
				addToken(DOT);
				break;
			case '-':
				addToken(MINUS);
				break;
			case '+':
				addToken(PLUS);
				break;
			case ';':
				addToken(SEMICOLON);
				break;
			case '*':
				addToken(STAR);
				break;
			
			//region Operators
			case '!':
				addToken(match('=') ? BANG_EQUAL : BANG);
				break;
			case '=':
				addToken(match('=') ? EQUAL_EQUAL : EQUAL);
				break;
			case '<':
				addToken(match('=') ? LESS_EQUAL : LESS);
				break;
			case '>':
				addToken(match('=') ? GREATER_EQUAL : GREATER);
				break;
			//endregion
			
			case '/':
				if (match('/'))
				{
					// A comment goes until the end of the line.
					while (peek() != '\n' && !isAtEnd())
						advance();
				}
				else
				{
					addToken(SLASH);
				}
				break;
			
			//region Meaningless characters
			case ' ':
			case '\r':
			case '\t':
				// Ignore whitespace.
				break;
			case '\n':
				line++;
				break;
			//endregion
			
			//region String literals
			case '"':
				string();
				break;
			//endregion
			
			default:
				if (isDigit(c))
				{
					number();
				}
				else
				{
					if (isAlpha(c))
					{
						identifier();
					}
					else
					{
						Lox.error(line, "Unexpected character.");
					}
				}
				break;
		}
	}
	
	private char advance()
	{
		return source.charAt(current++);
	}
	
	private void addToken(TokenType type)
	{
		addToken(type, null);
	}
	
	private void addToken(TokenType type, Object literal)
	{
		String text = source.substring(start, current);
		
		tokens.add(new Token(type, text, literal, line));
	}
	
	private boolean match(char expected)
	{
		if (isAtEnd())
			return false;
		
		if (source.charAt(current) != expected)
			return false;
		
		current++;
		
		return true;
	}
	
	private char peek() // looks ahead without consuming the character
	{
		if (isAtEnd())
			return '\0';
		
		return source.charAt(current);
	}
	
	private void string()
	{
		while (peek() != '"' && !isAtEnd())
		{
			if (peek() == '\n')
				line++;
			
			advance();
		}
		
		if (isAtEnd())
		{
			Lox.error(line, "Unterminated string.");
			return;
		}
		
		// The closing ".
		advance();
		
		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		
		addToken(STRING, value);
	}
	
	private boolean isDigit(char c)
	{
		return c >= '0' && c <= '9';
	}
	
	private void number()
	{
		while (isDigit(peek()))
			advance();
		
		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext()))
		{
			// Consume the "."
			advance();
			
			while (isDigit(peek()))
				advance();
		}
		
		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	
	private char peekNext()
	{
		if (current + 1 >= source.length())
			return '\0';
		
		return source.charAt(current + 1);
	}
	
	private void identifier()
	{
		while (isAlphaNumeric(peek()))
			advance();
		
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		
		if (type == null)
			type = IDENTIFIER;
		
		addToken(type);
	}
	
	private boolean isAlpha(char c)
	{
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}
	
	private boolean isAlphaNumeric(char c)
	{
		return isAlpha(c) || isDigit(c);
	}
}