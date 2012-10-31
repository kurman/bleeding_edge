// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * This provides a basic example of internationalization usage. It uses the
 * local variant of all the facilities, meaning that libraries with the
 * data for all the locales are directly imported by the program. More realistic
 * examples might read the data from files or over the web, which uses the
 * same APIs but requires different imports.
 *
 * This defines messages for an English locale directly in the program and
 * has separate libraries that define German and Thai messages that say more or
 * less the same thing, and prints the message with the date and time in it
 * formatted appropriately for the locale.
 */

library intl_basic_example;
// These can be replaced with package:intl/... references if using this in
// a separate package.
// TODO(alanknight): Replace these with package: once pub works in buildbots.
import '../../lib/date_symbol_data_local.dart';
import '../../lib/intl.dart';
import '../../lib/message_lookup_local.dart';
import 'messages_all.dart';

Function doThisWithTheOutput;

void setup(Function program, Function output) {
  // Before we use any messages or use date formatting for a locale we must
  // call their initializtion messages, which are asynchronous, since they
  // might be reading information from files or over the web. Since we are
  // running here in local mode they will all complete immediately.
  doThisWithTheOutput = output;
  var germanDatesFuture = initializeDateFormatting('de_DE', null);
  var thaiDatesFuture = initializeDateFormatting('th_TH', null);
  var germanMessagesFuture = initializeMessages('de_DE');
  var thaiMessagesFuture = initializeMessages('th_TH');
  Futures.wait([germanDatesFuture, thaiDatesFuture, germanMessagesFuture,
                thaiMessagesFuture]).then(program);
}

// Because the initialization messages return futures we split out the main
// part of our program into a separate function that runs once all the
// futures have completed. We are passed the collection of futures, but we
// don't need to use them, so ignore the parameter.
runProgram(List<Future> _) {
  var aDate = new Date.fromMillisecondsSinceEpoch(0, isUtc: true);
  var de = new Intl('de_DE');
  var th = new Intl('th_TH');
  // This defines a message that can be internationalized. It is written as a
  // function that returns the result of an Intl.message call. The primary
  // parameter is a string that may use interpolation.
  runAt(time, date) =>
      Intl.message('Ran at $time on $date', name: 'runAt', args: [time, date]);
  printForLocale(aDate, new Intl(), runAt);
  printForLocale(aDate, de, runAt);
  printForLocale(aDate, th, runAt);
  // Example making use of the return value from withLocale;
  var useReturnValue = Intl.withLocale(th.locale, () => runAt('now', 'today'));
  doThisWithTheOutput(useReturnValue);
}

printForLocale(aDate, intl, operation) {
  var hmsFormat = intl.date().add_Hms();
  var dayFormat = intl.date().add_yMMMMEEEEd();
  var time = hmsFormat.format(aDate);
  var day = dayFormat.format(aDate);
  Intl.withLocale(intl.locale, () { doThisWithTheOutput(operation(time,day));});
}