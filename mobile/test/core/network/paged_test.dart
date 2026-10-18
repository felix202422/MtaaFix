import 'package:flutter_test/flutter_test.dart';

import 'package:mtaafix_mobile/core/network/paged.dart';

void main() {
  Map<String, dynamic> item(int n) => {'id': 'r$n', 'value': n};

  int parseValue(Map<String, dynamic> json) => json['value'] as int;

  test('parses the flat PagedResponse shape', () {
    final paged = Paged.parse(
      {
        'content': [item(1), item(2)],
        'totalElements': 42,
        'totalPages': 3,
        'page': 1,
        'size': 20,
      },
      parseValue,
    );

    expect(paged.content, [1, 2]);
    expect(paged.totalElements, 42);
    expect(paged.totalPages, 3);
    expect(paged.page, 1);
    expect(paged.size, 20);
    expect(paged.hasMore, isTrue);
    expect(paged.isEmpty, isFalse);
  });

  test('parses Spring Page shape with number instead of page', () {
    final paged = Paged.parse(
      {
        'content': [item(7)],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
        'size': 20,
      },
      parseValue,
    );

    expect(paged.content, [7]);
    expect(paged.page, 0);
    expect(paged.totalPages, 1);
    expect(paged.hasMore, isFalse);
  });

  test('parses the nested V2 page block', () {
    final paged = Paged.parse(
      {
        'content': [item(3), item(4), item(5)],
        'page': {
          'number': 2,
          'totalPages': 5,
          'totalElements': 90,
          'size': 20,
        },
      },
      parseValue,
    );

    expect(paged.content, [3, 4, 5]);
    expect(paged.page, 2);
    expect(paged.totalPages, 5);
    expect(paged.totalElements, 90);
    expect(paged.size, 20);
    expect(paged.hasMore, isTrue);
  });

  test('falls back gracefully when pagination metadata is missing', () {
    final paged = Paged.parse(
      {'content': [item(9)]},
      parseValue,
    );

    expect(paged.content, [9]);
    expect(paged.page, 0);
    expect(paged.totalElements, 1);
    expect(paged.totalPages, 1);
    expect(paged.size, 1);
  });

  test('ignores malformed content entries', () {
    final paged = Paged.parse(
      {
        'content': ['not-a-map', item(2), 42],
        'totalElements': 1,
        'totalPages': 1,
        'page': 0,
        'size': 20,
      },
      parseValue,
    );

    expect(paged.content, [2]);
  });

  test('empty content parses to an empty page', () {
    final paged = Paged.parse(
      {'content': <dynamic>[], 'totalElements': 0, 'totalPages': 0, 'page': 0, 'size': 20},
      parseValue,
    );

    expect(paged.isEmpty, isTrue);
    expect(paged.hasMore, isFalse);
  });
}
