# Generated by Django 3.1.3 on 2021-03-21 01:28

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0007_auto_20210321_0112'),
    ]

    operations = [
        migrations.AddField(
            model_name='mapping',
            name='email',
            field=models.CharField(default='', max_length=128),
            preserve_default=False,
        ),
    ]