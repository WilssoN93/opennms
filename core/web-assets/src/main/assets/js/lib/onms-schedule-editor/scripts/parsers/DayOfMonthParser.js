/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
import Types from '../Types';
import Time from '../Time';
import CronDefinition from '../CronDefintion';
import ScheduleOptions from '../ScheduleOptions';

/**
 * The DayOfMonthParser will parse any cron expression,
 * which was generated by the Schedule Editor in "Days of Month" mode.
 */
export default class DayOfMonthParser {
    constructor() {
        this.regExp = new RegExp('[1-7]#[1,2,3]|L');
    }

    canParse(input) {
        const cron = CronDefinition.createFrom(input);
        const canParse = cron.year === undefined
            && cron.seconds === '0'
            && cron.isConcreteMinutes()
            && cron.isConcreteHours()
            && cron.month === '*'
            && cron.dayOfMonth.indexOf(',') === -1
            && cron.dayOfMonth.indexOf('-') === -1
            && cron.dayOfMonth.indexOf('/') === -1
            // If dayOfMonth is set, dayOfWeek must be ?
            // Or dayOfMonth is ?, then dayOfWeek must be set, only then it is parsable
            && (((parseInt(cron.dayOfMonth, 10) >= 1  && parseInt(cron.dayOfMonth, 10) <= 31)
                    || cron.dayOfMonth === 'L' && cron.dayOfWeek === '?')
                || (cron.dayOfMonth === '?' && this.regExp.test(cron.dayOfWeek)));
        return canParse;
    }

    parse(input) {
        const cron = CronDefinition.createFrom(input);
        const options = new ScheduleOptions({
            type: Types.DAYS_PER_MONTH,
            at: new Time({ hours: cron.hours, minutes: cron.minutes })
        });

        // Determine the toggle
        if (cron.dayOfMonth === '?') {
            options.dayOfMonthToggle = 'dayOfWeek';
        } else {
            options.dayOfMonthToggle = 'dayOfMonth';
        }

        // Set the values according of the toggle
        if (options.dayOfMonthToggle === 'dayOfMonth') {
            options.dayOfMonth = cron.dayOfMonth;
        } else {
            options.weekOfMonth = cron.dayOfWeek.substr(-1);
            options.dayOfWeek = cron.dayOfWeek.substr(0, 1);
        }
        return options;
    }
}