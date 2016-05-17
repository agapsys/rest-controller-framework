/*
 * Copyright 2016 Agapsys Tecnologia Ltda-ME.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agapsys.rcf.exceptions;

/**
 *
 * @author Leandro Oliveira (leandro@agapsys)
 */
public class CheckedException extends Exception {
	
	public CheckedException() {}

	public CheckedException(String msg, Object...msgArgs) {
		super(msgArgs.length > 0 ? String.format(msg, msgArgs) : msg);
	}

	public CheckedException(Throwable throwable) {
		super(throwable);
	}

	public CheckedException(Throwable throwable, String msg, Object...msgArgs) {
		super(msgArgs.length > 0 ? String.format(msg, msgArgs) : msg, throwable);
	}

}