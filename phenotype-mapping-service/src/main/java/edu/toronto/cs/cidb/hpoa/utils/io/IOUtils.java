/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.cidb.hpoa.utils.io;

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class IOUtils {
	public static File getInputFileHandler(String inputLocation) {
		return (getInputFileHandler(inputLocation, false));
	}

	public static File getInputFileHandler(String inputLocation,
			boolean forceUpdate) {
		try {
			File result = new File(inputLocation);
			if (!result.exists()) {
				// System.err.println("File <" + inputLocation +
				// "> does not exist locally.");
				// maybe it's an external url?
				String name = inputLocation.substring(inputLocation
						.lastIndexOf('/') + 1);
				result = new File(getTemporaryFilePath(name));
				if (!result.exists()) {
					result.createNewFile();
					BufferedInputStream in = new BufferedInputStream((new URL(
							inputLocation)).openStream());
					OutputStream out = new FileOutputStream(result);

					byte[] buf = new byte[1024];
					int len;

					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					out.flush();
					out.close();
				}
			}
			return result;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static File getOutputFileHandler(String outputLocation) {
		return new File(outputLocation);
	}

	protected static String getResourceFilePath(String name) {
		return getInternalFilePath(name, "res");
	}

	protected static String getTemporaryFilePath(String name) {
		return getInternalFilePath(name, "tmp");
	}

	protected static String getInternalFilePath(String name, String dir) {
		return (new File("")).getAbsolutePath() + File.separator + dir
				+ File.separator + name;
	}
}
