/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web.sstore.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.shareddata.ClusterSerializable;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.web.sstore.AbstractSession;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SharedDataSessionImpl extends AbstractSession implements ClusterSerializable, Shareable {

  private static final Charset UTF8 = StandardCharsets.UTF_8;

  /**
   * Important note: This constructor (even though not referenced anywhere) is required for serialization purposes. Do
   * not remove.
   */
  public SharedDataSessionImpl() {
    super();
  }

  public SharedDataSessionImpl(VertxContextPRNG random) {
    super(random);
  }

  public SharedDataSessionImpl(VertxContextPRNG random, long timeout, int length) {
    super(random, timeout, length);
  }

  @Override
  public void writeToBuffer(Buffer buff) {
    byte[] bytes = id().getBytes(UTF8);
    buff.appendInt(bytes.length).appendBytes(bytes);
    buff.appendLong(timeout());
    buff.appendLong(lastAccessed());
    buff.appendInt(version());
    buff.appendInt(isDestroyed() ? 1 : 0);
    buff.appendInt(isRegenerated() ? 1 : 0);
    bytes = oldId() != null ? oldId().getBytes(UTF8) : new byte[0];
    buff.appendInt(bytes.length).appendBytes(bytes);
    writeDataToBuffer(buff);
  }

  @Override
  public int readFromBuffer(int pos, Buffer buffer) {
    // id length and value
    int len = buffer.getInt(pos);
    pos += 4;
    byte[] bytes = buffer.getBytes(pos, pos + len);
    pos += len;
    setId(new String(bytes, UTF8));
    // timeout
    setTimeout(buffer.getLong(pos));
    pos += 8;
    // last accessed
    setLastAccessed(buffer.getLong(pos));
    pos += 8;
    // version
    setVersion(buffer.getInt(pos));
    pos += 4;
    // destroyed
    setDestroyed(buffer.getInt(pos) == 1 ? true : false);
    pos += 4;
    // renewed
    setRenewed(buffer.getInt(pos) == 1 ? true : false);
    pos += 4;
    // oldId length and value
    len = buffer.getInt(pos);
    pos += 4;
    bytes = buffer.getBytes(pos, pos + len);
    pos += len;
    setOldId(len != 0 ? new String(bytes, UTF8) : null);
    // data
    pos = readDataFromBuffer(pos, buffer);
    return pos;
  }
}
