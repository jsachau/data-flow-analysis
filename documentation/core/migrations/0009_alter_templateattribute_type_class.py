# Generated by Django 3.2 on 2021-04-11 13:35

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0008_mapping_email'),
    ]

    operations = [
        migrations.AlterField(
            model_name='templateattribute',
            name='type_class',
            field=models.ForeignKey(blank=True, default=None, null=True, on_delete=django.db.models.deletion.SET_NULL, to='core.templateclass'),
        ),
    ]
